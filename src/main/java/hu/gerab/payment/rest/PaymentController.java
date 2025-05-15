package hu.gerab.payment.rest;

import static java.math.BigDecimal.ZERO;

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
  public String handleTransfer(Long senderId, Long receiverId, BigDecimal amount, String currency) {
    Currency validatedCurrency;
    try {
      // we have to leave this case sensitive, for currencies like GBP and GBp where the minor
      // currency exists and is used but there is only a case difference
      validatedCurrency = Currency.valueOf(currency.trim());
    } catch (IllegalArgumentException e) { // this will be a currency validation exception
      throw new IllegalArgumentException(currency + " currency is not supported");
    }

    if (ZERO.compareTo(amount) >= 0) {
      throw new IllegalArgumentException("Transfer requires a positive amount");
    }
    if (senderId.equals(receiverId)) {
      throw new IllegalArgumentException("Sender and Receiver user cannot be the same.");
    }
    String requestId = UUID.randomUUID().toString();
    paymentService.processTransfer(requestId, senderId, receiverId, amount, validatedCurrency);
    return requestId;
  }
}
