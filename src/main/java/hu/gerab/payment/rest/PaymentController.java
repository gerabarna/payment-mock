package hu.gerab.payment.rest;

import static java.math.BigDecimal.ZERO;

import hu.gerab.payment.Comparables;
import hu.gerab.payment.domain.Currency;
import hu.gerab.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController implements PaymentAPI {

  private final PaymentService paymentService;

  @Autowired
  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @Override
  public String handleTransaction(Long userId, BigDecimal amount, String currency) {
    Currency validatedCurrency;
    try {
      // we have to leave this case sensitive, for currencies like GBP and GBp where the minor
      // currency exists and is used but there is only a case difference
      validatedCurrency = Currency.valueOf(currency.trim());
    } catch (IllegalArgumentException e) { // this will be a currency validation exception
      throw new IllegalArgumentException("Only USD transactions are accepted");
    }
    if (Comparables.compareEquals(amount, ZERO)) {
      throw new IllegalArgumentException("Transactions should have a non-zero amount.");
    }
    String requestId = UUID.randomUUID().toString();
    paymentService.processTransaction(requestId, userId, amount, validatedCurrency);
    return requestId;
  }
}
