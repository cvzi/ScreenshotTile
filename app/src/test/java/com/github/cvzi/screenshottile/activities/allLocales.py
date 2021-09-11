locales = [
    "ar",
    "ar-rSA",
    "bn",
    "de-rDE",
    "el-rGR",
    "es",
    "es-rES",
    "fa",
    "fr-rFR",
    "gu",
    "hi",
    "hi-rIN",
    "hu-rHU",
    "hy-rAM",
    "in",
    "it-rIT",
    "iw",
    "ja",
    "kn",
    "mr",
    "ms",
    "nb",
    "nl-rNL",
    "no",
    "or",
    "pa",
    "pl",
    "pl-rPL",
    "pt-rBR",
    "pt-rPT",
    "ro-rRO",
    "ru",
    "ru-rRU",
    "sw",
    "ta",
    "te",
    "th",
    "tl",
    "tr-rTR",
    "ug",
    "uk-rUA",
    "ur",
    "vi-rVN",
    "zh",
    "zh-rCN",
    "zh-rHK",
    "zh-rTW"]

keywords = [
    # https://github.com/JetBrains/kotlin/blob/v1.4.10/core/descriptors/src/org/jetbrains/kotlin/renderer/KeywordStringsGenerated.java
    "package",
    "as",
    "typealias",
    "class",
    "this",
    "super",
    "val",
    "var",
    "fun",
    "for",
    "null",
    "true",
    "false",
    "is",
    "in",
    "throw",
    "return",
    "break",
    "continue",
    "object",
    "if",
    "try",
    "else",
    "while",
    "do",
    "when",
    "interface",
    "typeof"
]


def methodName(locale):
    x = locale.replace('-r', '').replace('-', '')
    while x in keywords:
        x += "99"
    return x


for locale in locales:
    print('''
    @Test
    @Config(qualifiers="%s")
    fun %s() {
        readStringFromContext_LocalizedString()
    }''' % (locale, methodName(locale)))
