package hu.gerab.payment.rest;

import hu.gerab.payment.Comparables;
import hu.gerab.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import static java.math.BigDecimal.ZERO;

@RestController
public class PaymentController implements PaymentAPI {

  private final PaymentService paymentService;

  @Autowired
  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @Override
  public String handleTransaction(Long userId, BigDecimal amount, String currency) {
    currency = currency.trim().toUpperCase();
    if (!"USD".equalsIgnoreCase(currency)) {
      throw new IllegalArgumentException("Only USD transactions are accepted");
    }
    if (Comparables.compareEquals(amount, ZERO)) {
      throw new IllegalArgumentException("Transactions should have a non-zero amount.");
    }
    String requestId = UUID.randomUUID().toString();
    paymentService.processTransaction(requestId, userId, amount, currency);
    return requestId;
  }
}
