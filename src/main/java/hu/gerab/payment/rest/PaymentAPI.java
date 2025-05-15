package hu.gerab.payment.rest;

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.QueryParam;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("payment")
@Tag(name = "Payment")
// @SecurityRequirement() TBD
public interface PaymentAPI {

  /**
   * For simple invocation please send a POST request to:
   * localhost:8080/payment/trasnfer?senderId=2&receiverId=4&amount=10&currency=USD
   */
  @Operation(
      summary = "Handles a simple transaction for the user",
      responses = {
        @ApiResponse(content = @Content(schema = @Schema(implementation = String.class)))
      })
  @PostMapping("transfer")
  String handleTransfer(
      @Parameter(description = "The id of the user sending the money", in = QUERY)
          @QueryParam("senderId")
          Long senderId,
      @Parameter(description = "The id of the user receiving the money", in = QUERY)
          @QueryParam("receiverId")
          Long receiverId,
      @Parameter(
              description =
                  "The amount with which to change the user balance. Only non-zero amounts are valid",
              in = QUERY)
          @QueryParam("amount")
          BigDecimal amount,
      @Parameter(
              description =
                  "The currency of the transaction. Only USD transactions are accepted for now",
              in = QUERY)
          @QueryParam("currency")
          String currency);
}
