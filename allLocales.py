import os
import re

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

locales = []


def methodName(locale):
    x = locale.replace('-r', '').replace('-', '')
    while x in keywords:
        x += "99"
    return x


print('            - "en-US"')
for filename in os.listdir("app/src/main/res"):
    m = re.match("^values-([a-z]{2})-r([A-Z]{2})$", filename)
    m2 = re.match("^values-([a-z]{2})$", filename)
    if m:
        print('            - "%s-%s"' % (m.group(1), m.group(2)))

    if m or m2:
        locales.append(filename.split("values-")[1])


for locale in locales:
    print('''
    @Test
    @Config(qualifiers="%s")
    fun %s() {
        readStringFromContext_LocalizedString()
    }''' % (locale, methodName(locale)))
