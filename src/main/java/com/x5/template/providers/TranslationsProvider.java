package com.x5.template.providers;

import java.util.Map;

public interface TranslationsProvider {
    Map<String,String> getTranslations(String localeCode);
}
