package io.nexstudios.framework.core.service.language;

import io.nexstudios.serviceregistry.di.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public interface LanguageService extends Service {

  String getTranslation(String translationKey);

  String getTranslation(Locale locale, String translationKey);

  String getTranslation(UUID userIdentifier, String translationKey);

  void setDefaultLocale(Locale locale);

  Locale getDefaultLocale();

  void setUserLocale(UUID userIdentifier, Locale locale);

  Locale getUserLocale(UUID userIdentifier);

  Set<Locale> getAvailableLocales();

  void reload();
}