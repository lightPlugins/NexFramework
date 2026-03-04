package io.nexstudios.framework.paper.services.commands.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandRoot {
  String name();
  String description() default "";
  String[] aliases() default {};
  String permission() default "";
  boolean playerOnly() default false;
}