package splitwise.util.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import splitwise.dto.GroupMember;

public class EitherPhoneOrEmailValidator implements ConstraintValidator<EitherPhoneOrEmailRequired, GroupMember> {

    @Override
    public boolean isValid(GroupMember member, ConstraintValidatorContext context) {
        if (member == null) return true;

        boolean hasPhone = member.getMobile() != null && !member.getMobile().isBlank();
        boolean hasEmail = member.getEmail() != null && !member.getEmail().isBlank();

        // Rule 1: at least one contact info must exist
        if (!hasPhone && !hasEmail) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Either phone or email must be provided")
                    .addPropertyNode("phone")
                    .addConstraintViolation();
            return false;
        }

        // Rule 2: if id is null (new user), phone must not be null
        if (member.getUserId() == null && !hasPhone) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Phone is required when id is null")
                    .addPropertyNode("phone")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
