package uy.edu.um.proytic2026r.control;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

// Simulador de Mercado Pago: mismos endpoints y mismo formato de respuesta que la API real.
// Solo se usa cuando no hay Access Token configurado. El "token" de tarjeta del simulador
// tiene la forma SIM-<numero de tarjeta>.
@RestController
@RequestMapping("/mock-mercadopago")
public class MercadoPagoSimuladoControl {

    private final AtomicLong secuencia = new AtomicLong(1325000000L);
    private final Random random = new Random();

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new HashMap<String, Object>();
        cuerpo.put("message", mensaje);
        cuerpo.put("status", estado.value());
        return ResponseEntity.status(estado).body(cuerpo);
    }

    @PostMapping("/v1/payments")
    public ResponseEntity<Map<String, Object>> crearPago(
            @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody Map<String, Object> pedido) {
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
            return error(HttpStatus.UNAUTHORIZED, "invalid access token");
        }
        Object token = pedido.get("token");
        Object monto = pedido.get("transaction_amount");
        if (token == null || monto == null || pedido.get("payment_method_id") == null) {
            return error(HttpStatus.BAD_REQUEST, "token, transaction_amount y payment_method_id son obligatorios");
        }
        String digitos = String.valueOf(token).replaceAll("\\D", "");
        if (digitos.length() < 13 || digitos.length() > 19) {
            return error(HttpStatus.BAD_REQUEST, "invalid card token");
        }

        String estado;
        String detalle;
        if (digitos.equals("4000000000000119")) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "service unavailable");
        } else if (digitos.equals("4242424242424242")) {
            estado = "approved";
            detalle = "accredited";
        } else if (digitos.equals("4000000000000002")) {
            estado = "rejected";
            detalle = "cc_rejected_other_reason";
        } else if (digitos.equals("4000000000009995")) {
            estado = "rejected";
            detalle = "cc_rejected_insufficient_amount";
        } else if (digitos.equals("4000000000000077")) {
            estado = "in_process";
            detalle = "pending_contingency";
        } else {
            int sorteo = random.nextInt(100);
            if (sorteo < 75) {
                estado = "approved";
                detalle = "accredited";
            } else if (sorteo < 95) {
                estado = "rejected";
                detalle = "cc_rejected_other_reason";
            } else {
                return error(HttpStatus.SERVICE_UNAVAILABLE, "service unavailable");
            }
        }

        Map<String, Object> tarjeta = new HashMap<String, Object>();
        tarjeta.put("last_four_digits", digitos.substring(digitos.length() - 4));

        Map<String, Object> respuesta = new HashMap<String, Object>();
        respuesta.put("id", secuencia.incrementAndGet());
        respuesta.put("status", estado);
        respuesta.put("status_detail", detalle);
        respuesta.put("transaction_amount", monto);
        respuesta.put("currency_id", "UYU");
        respuesta.put("payment_method_id", pedido.get("payment_method_id"));
        respuesta.put("external_reference", pedido.get("external_reference"));
        respuesta.put("card", tarjeta);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/v1/payments/{id}/refunds")
    public ResponseEntity<Map<String, Object>> reembolsar(
            @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @PathVariable String id,
            @RequestBody Map<String, Object> pedido) {
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
            return error(HttpStatus.UNAUTHORIZED, "invalid access token");
        }
        Map<String, Object> respuesta = new HashMap<String, Object>();
        respuesta.put("id", secuencia.incrementAndGet());
        respuesta.put("payment_id", id);
        respuesta.put("amount", pedido.get("amount"));
        respuesta.put("status", "approved");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
