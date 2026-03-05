package io.nexstudios.framework.core.service.component;

import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Dependencies({
    LanguageService.class
})
public final class DefaultComponentService implements ComponentService {

  private final LanguageService languageService;
  private final MiniMessage miniMessage;

  public DefaultComponentService(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");
    this.languageService = services.getService(LanguageService.class);

    this.miniMessage = MiniMessage.builder()
        // default mini message settings / formatting's
        .postProcessor(component -> component.decoration(TextDecoration.ITALIC, false))
        .build();
  }

  @Override
  public Component resolve(String translationKey) {
    return resolve(languageService.getDefaultLocale(), translationKey, TagResolver.empty(), null);
  }

  @Override
  public Component resolve(String translationKey, TagResolver resolver) {
    return resolve(languageService.getDefaultLocale(), translationKey, resolver, null);
  }

  @Override
  public Component resolve(String translationKey, TagResolver resolver, Component prefix) {
    return resolve(languageService.getDefaultLocale(), translationKey, resolver, prefix);
  }

  @Override
  public Component resolve(Locale locale, String translationKey) {
    return resolve(locale, translationKey, TagResolver.empty(), null);
  }

  @Override
  public Component resolve(Locale locale, String translationKey, TagResolver resolver) {
    return resolve(locale, translationKey, resolver, null);
  }

  @Override
  public Component resolve(Locale locale, String translationKey, TagResolver resolver, Component prefix) {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(translationKey, "translationKey");
    Objects.requireNonNull(resolver, "resolver");

    String text = languageService.getTranslation(locale, translationKey);

    Component parsed = miniMessage.deserialize(text, resolver);

    if (prefix == null) {
      return parsed;
    }
    return prefix.append(parsed);
  }

  @Override
  public Component resolve(UUID userIdentifier, String translationKey) {
    return resolve(userIdentifier, translationKey, TagResolver.empty(), null);
  }

  @Override
  public Component resolve(UUID userIdentifier, String translationKey, TagResolver resolver) {
    return resolve(userIdentifier, translationKey, resolver, null);
  }

  @Override
  public Component resolve(UUID userIdentifier, String translationKey, TagResolver resolver, Component prefix) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Locale locale = languageService.getUserLocale(userIdentifier);
    return resolve(locale, translationKey, resolver, prefix);
  }
}