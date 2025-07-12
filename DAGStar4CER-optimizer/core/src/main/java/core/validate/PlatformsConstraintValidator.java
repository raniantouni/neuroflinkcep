package core.validate;

import core.parser.dictionary.Platforms;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PlatformsConstraintValidator implements ConstraintValidator<PlatformsConstraint, Platforms> {

    @Override
    public boolean isValid(Platforms platforms, ConstraintValidatorContext context) {
        return platforms.getAkka() != null || platforms.getFlink() != null || platforms.getSpark() != null;
    }
}
