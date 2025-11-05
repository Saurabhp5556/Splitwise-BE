package splitwise.util.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EitherPhoneOrEmailValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EitherPhoneOrEmailRequired {

    String message() default "Invalid member: either phone or email must be provided, and phone is required if id is null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
