package uy.edu.um.proytic2026r.control;

import uy.edu.um.proytic2026r.servicio.PaymentGatewayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Le dice al index si el pago es con Mercado Pago real (y su Public Key, que es publica) o simulado.
@RestController
@RequestMapping("/api/payments")
public class PagoConfigControl {

    private final PaymentGatewayService pasarela;

    public PagoConfigControl(PaymentGatewayService pasarela) {
        this.pasarela = pasarela;
    }

    @GetMapping("/config")
    public Map<String, Object> configuracion() {
        return pasarela.configuracion();
    }
}
