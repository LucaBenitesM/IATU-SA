package uy.edu.um.proytic2026r.servicio;

import uy.edu.um.proytic2026r.entities.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Cliente de la API de pagos de Mercado Pago (POST /v1/payments y /v1/payments/{id}/refunds).
// Si no hay Access Token configurado, le habla al simulador interno (/mock-mercadopago),
// que responde con el mismo formato que Mercado Pago.
@Service
public class PaymentGatewayService {

    public static class NoDisponibleException extends RuntimeException {
        public NoDisponibleException(String mensaje) {
            super(mensaje);
        }
    }

    public static class Resultado {
        private final String estado;     // Payment.APPROVED, REJECTED o IN_PROCESS
        private final String idExterno;  // id del pago en Mercado Pago
        private final String detalle;    // status_detail de Mercado Pago
        private final String ultimos4;

        public Resultado(String estado, String idExterno, String detalle, String ultimos4) {
            this.estado = estado;
            this.idExterno = idExterno;
            this.detalle = detalle;
            this.ultimos4 = ultimos4;
        }

        public String getEstado() { return estado; }
        public String getIdExterno() { return idExterno; }
        public String getDetalle() { return detalle; }
        public String getUltimos4() { return ultimos4; }
    }

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.public-key:}")
    private String publicKey;

    @Value("${mercadopago.base-url:https://api.mercadopago.com}")
    private String baseUrl;

    @Value("${server.port:8080}")
    private String puerto;

    private final RestClient cliente;

    public PaymentGatewayService() {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(15));
        this.cliente = RestClient.builder().requestFactory(fabrica).build();
    }

    public boolean esReal() {
        return accessToken != null && !accessToken.trim().isEmpty();
    }

    public Map<String, Object> configuracion() {
        Map<String, Object> conf = new HashMap<String, Object>();
        boolean real = esReal() && publicKey != null && !publicKey.trim().isEmpty();
        conf.put("mode", real ? "mercadopago" : "simulado");
        conf.put("publicKey", real ? publicKey.trim() : null);
        return conf;
    }

    private String urlBase() {
        if (esReal()) {
            return baseUrl;
        }
        return "http://localhost:" + puerto + "/mock-mercadopago";
    }

    private String token() {
        return esReal() ? accessToken.trim() : "TEST-simulado";
    }

    // datos: lo que devuelve el formulario de tarjeta (token, payment_method_id, installments, issuer_id, payer)
    public Resultado cobrar(BigDecimal monto, String descripcion, String referencia, Map<String, Object> datos) {
        Map<String, Object> cuerpo = new HashMap<String, Object>();
        cuerpo.put("transaction_amount", monto);
        cuerpo.put("token", datos.get("token"));
        cuerpo.put("description", descripcion);
        cuerpo.put("installments", datos.get("installments") != null ? datos.get("installments") : 1);
        cuerpo.put("payment_method_id", datos.get("payment_method_id"));
        if (datos.get("issuer_id") != null) {
            cuerpo.put("issuer_id", datos.get("issuer_id"));
        }
        cuerpo.put("payer", pagador(datos.get("payer")));
        cuerpo.put("external_reference", referencia);

        Map<String, Object> r = enviar(urlBase() + "/v1/payments", cuerpo);
        if (r == null || r.get("status") == null) {
            throw new NoDisponibleException("Respuesta vacia de Mercado Pago.");
        }
        String status = String.valueOf(r.get("status"));
        String estado;
        if ("approved".equals(status)) {
            estado = Payment.APPROVED;
        } else if ("in_process".equals(status) || "pending".equals(status) || "authorized".equals(status)) {
            estado = Payment.IN_PROCESS;
        } else {
            estado = Payment.REJECTED;
        }
        String detalle = r.get("status_detail") == null ? null : String.valueOf(r.get("status_detail"));
        String id = r.get("id") == null ? null : String.valueOf(r.get("id"));
        String ultimos4 = null;
        Object tarjeta = r.get("card");
        if (tarjeta instanceof Map) {
            Object l4 = ((Map<?, ?>) tarjeta).get("last_four_digits");
            ultimos4 = l4 == null ? null : String.valueOf(l4);
        }
        return new Resultado(estado, id, detalle, ultimos4);
    }

    public void reembolsar(String idPago, BigDecimal monto) {
        Map<String, Object> cuerpo = new HashMap<String, Object>();
        cuerpo.put("amount", monto);
        enviar(urlBase() + "/v1/payments/" + idPago + "/refunds", cuerpo);
    }

    // Solo se reenvian el email y la identificacion del pagador
    private Map<String, Object> pagador(Object origen) {
        Map<String, Object> pagador = new HashMap<String, Object>();
        if (origen instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) origen;
            if (m.get("email") != null) {
                pagador.put("email", m.get("email"));
            }
            if (m.get("identification") instanceof Map) {
                Map<?, ?> ident = (Map<?, ?>) m.get("identification");
                Map<String, Object> identificacion = new HashMap<String, Object>();
                if (ident.get("type") != null) {
                    identificacion.put("type", ident.get("type"));
                }
                if (ident.get("number") != null) {
                    identificacion.put("number", ident.get("number"));
                }
                pagador.put("identification", identificacion);
            }
        }
        return pagador;
    }

    private Map<String, Object> enviar(String url, Map<String, Object> cuerpo) {
        try {
            return cliente.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token())
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        } catch (RestClientResponseException e) {
            throw new NoDisponibleException("Mercado Pago respondio " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new NoDisponibleException("No se pudo conectar con Mercado Pago: " + e.getMessage());
        }
    }
}
