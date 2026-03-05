package io.nexstudios.framework.core.service.component;

import io.nexstudios.serviceregistry.di.Service;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Locale;
import java.util.UUID;

public interface ComponentService extends Service {

  Component resolve(String translationKey);

  Component resolve(String translationKey, TagResolver resolver);

  Component resolve(String translationKey, TagResolver resolver, Component prefix);

  Component resolve(Locale locale, String translationKey);

  Component resolve(Locale locale, String translationKey, TagResolver resolver);

  Component resolve(Locale locale, String translationKey, TagResolver resolver, Component prefix);

  Component resolve(UUID userIdentifier, String translationKey);

  Component resolve(UUID userIdentifier, String translationKey, TagResolver resolver);

  Component resolve(UUID userIdentifier, String translationKey, TagResolver resolver, Component prefix);
}