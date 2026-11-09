package uy.edu.um.proytic2026r.servicio;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Random;

@Service
public class PaymentGatewayService {

    public static class NoDisponibleException extends RuntimeException {
        public NoDisponibleException(String mensaje) {
            super(mensaje);
        }
    }

    private final Random random = new Random();

    // Devuelve "APPROVED" o "REJECTED". Si el servicio externo se cae, lanza NoDisponibleException.
    // 75% aprobado, 20% rechazado, 5% servicio no disponible.
    public String cobrar(BigDecimal monto) {
        int sorteo = random.nextInt(100);
        if (sorteo < 75) {
            return "APPROVED";
        }
        if (sorteo < 95) {
            return "REJECTED";
        }
        throw new NoDisponibleException("El servicio externo de pagos no responde.");
    }
}