package io.polygloat.collections;

import io.polygloat.model.Language;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class LanguageSet extends LinkedHashSet<Language> {

    public LanguageSet() {
        super();
    }

    public LanguageSet(Collection<? extends Language> c) {
        super(c);
    }

    public LanguageSet(SortedSet<Language> s) {
        super(s);
    }

    public Set<String> getAbbreviations() {
        return this.stream().map(Language::getAbbreviation).collect(Collectors.toSet());
    }

    /**
    *
    */
    private static final long serialVersionUID = -6613807560609280131L;
}
