"""
Take the strings from strings.xml that looks like this:


<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">&#xfffc;</string>
    <string name="main_general_text">&#xfffc;</string>
    <string name="more_setting">&#xfffc;</string>
</resources>

and convert it into a Kotlin class like this:
class Texts {
    val app_name = R.string.app_name
    val main_general_text = R.string.main_general_text
    val more_setting = R.string.more_setting
}
"""

import xml.etree.ElementTree as ET

def parse_strings_xml(file_path):
    tree = ET.parse(file_path)
    root = tree.getroot()
    strings = {}
    for string in root.findall('string'):
        name = string.get('name')
        strings[name] = f'R.string.{name}'
    return strings

def generate_kotlin_class(strings):
    class_content = """package com.github.cvzi.screenshottile.utils

import com.github.cvzi.screenshottile.R

class Texts {\n"""
    for name, value in strings.items():
        class_content += f"    val {name} = {value}\n"
    class_content += "}"
    return class_content

def main():
    input_file = 'app/src/main/res/values/strings.xml'
    output_file = 'Texts.kt'

    strings = parse_strings_xml(input_file)
    kotlin_class = generate_kotlin_class(strings)

    with open(output_file, 'w') as f:
        f.write(kotlin_class)

    print(f"Kotlin class generated and saved to {output_file}")

if __name__ == "__main__":
    main()
