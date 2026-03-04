package io.nexstudios.framework.paper.services.commands.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
  /**
   * Literal path relative to root, z.B.:
   * ""            -> root execution
   * "reload"      -> /root reload
   * "debug on"    -> /root debug on
   */
  String value();
  String permission() default "";
  boolean playerOnly() default false;
}