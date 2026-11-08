package uy.edu.um.proytic2026r.servicio;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Random;

@Service
public class PaymentGatewayService {

    /**
     * Simula la integración con una API externa de pagos de un tercero.
     * @return true si el pago fue aprobado, false si fue rechazado.
     * @throws RuntimeException si el servicio externo no está disponible (timeout/caída).
     */
    public boolean processPayment(Long bookingId, BigDecimal amount) {
        Random random = new Random();
        int simulation = random.nextInt(10); // Genera un número entre 0 y 9

        // 1. 70% de probabilidad de éxito (Pago aprobado)
        if (simulation < 7) {
            return true;
        }
        // 2. 20% de probabilidad de pago rechazado (Fondos insuficientes, tarjeta inválida, etc.)
        else if (simulation < 9) {
            return false;
        }
        // 3. 10% de probabilidad de simular que el API externa no responde o está caída
        else {
            throw new RuntimeException("Error de conexión: El servicio de pagos externo no se encuentra disponible temporalmente.");
        }
    }
}