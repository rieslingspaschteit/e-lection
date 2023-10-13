package pse.election.backendserver.payload.error;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pse.election.backendserver.payload.error.exception.EntityNotFoundException;
import pse.election.backendserver.payload.error.exception.IllegalStateSwitchOperation;
import pse.election.backendserver.payload.error.exception.ProviderNotFoundException;
import pse.election.backendserver.payload.error.exception.UnauthorizedAccessException;

/**
 * This class is used for handling exceptions and translate them into an HTTP-Error response for the
 * client with a body explaining the issue.
 *
 * @version 1.0
 */
@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LogManager.getLogger(ErrorHandler.class);

  /**
   * This method is called when any user principal tries to access an endpoint but the permission
   * evaluators or the security filter denied the access. It builds an HTTP-Response containing the
   * HTTP-FORBIDDEN status code.
   *
   * @param ex      is the exception that occurred
   * @param request is the request causing the error
   */
  @ExceptionHandler({UnauthorizedAccessException.class})
  public ResponseEntity<String> buildAccessDeniedResponse(Exception ex, WebRequest request) {
    logger.error(ex.getMessage());
    return new ResponseEntity<String>(ex.getMessage(), new HttpHeaders(), HttpStatus.FORBIDDEN);
  }

  /**
   * This method is called when any user tries to authenticate with a not supported OpenID Connect
   * provider. It builds an HTTP-Response containing the HTTP-NOT_FOUND status code.
   *
   * @param ex      is the exception that occurred
   * @param request is the request causing the error
   */
  @ExceptionHandler({ProviderNotFoundException.class})
  public ResponseEntity<String> buildAuthenticationFailedResponse(Exception ex,
      WebRequest request) {
    logger.error(ex.getMessage());
    return new ResponseEntity<String>(ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND);
  }

  /**
   * This method is called when any user tries to submit a resource to the server with an illegal
   * argument. It builds an HTTP-Response containing the HTTP-BAD_REQUEST status code.
   *
   * @param ex      is the exception that occurred
   * @param request is the request causing the error
   */
  @ExceptionHandler({IllegalArgumentException.class, NumberFormatException.class,
      EntityNotFoundException.class, IllegalStateSwitchOperation.class})
  public ResponseEntity<String> buildIllegalArgumentResponse(Exception ex, WebRequest request) {
    logger.error(ex.getMessage());
    return new ResponseEntity<String>(ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

}
