#!/usr/bin/env python3
#
# Check that translated strings use the same format arguments as the English ones
#
# A translation that reorders unnumbered placeholders (e.g., "%@ in %@ (%d / %d)" for
# "Syncing %@ (%d / %d) in %@") becomes a string whose argument types don't match the
# code, which crashes the app with an IllegalFormatConversionException. Every string in
# values-*/imported_strings*.xml is compared against values/, and mismatches are listed.

import os
import re
import sys
import xml.etree.ElementTree as ET

RES_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.realpath(__file__))), 'app', 'src', 'main', 'res')
FILES = ('imported_strings.xml', 'imported_strings_plural.xml')
SPECIFIER = re.compile(r'%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])')
TYPES = {'d': 'int', 'x': 'int', 'o': 'int', 's': 'string', 'f': 'float', 'e': 'float', 'g': 'float'}


def arg_types(text):
	"""Map each argument position to the type the string formats it as"""
	types = {}
	position = 0
	for match in SPECIFIER.finditer(text or ''):
		conversion = match.group(2)
		if conversion in ('%', 'n'):
			continue
		if match.group(1):
			index = int(match.group(1))
		else:
			position += 1
			index = position
		types.setdefault(index, set()).add(TYPES.get(conversion.lower(), conversion))
	return types


def load(dir_path):
	strings = {}
	for filename in FILES:
		path = os.path.join(dir_path, filename)
		if not os.path.exists(path):
			continue
		for element in ET.parse(path).getroot():
			name = element.get('name')
			if element.tag == 'string':
				strings[name] = [element.text]
			elif element.tag == 'plurals':
				strings[name] = [item.text for item in element]
	return strings


def compatible(english, translation):
	"""Every argument the translation uses must exist in the English with the same type.
	Plural forms may leave out the count."""
	for index, types in translation.items():
		if index not in english or not types <= english[index]:
			return False
	return True


def main():
	english = load(os.path.join(RES_DIR, 'values'))
	problems = []
	for folder in sorted(os.listdir(RES_DIR)):
		if not folder.startswith('values-'):
			continue
		for name, texts in load(os.path.join(RES_DIR, folder)).items():
			if name not in english:
				continue
			expected = {}
			for text in english[name]:
				for index, types in arg_types(text).items():
					expected.setdefault(index, set()).update(types)
			for text in texts:
				if not compatible(expected, arg_types(text)):
					problems.append(f'{folder}: {name}: {text!r} (English: {english[name][-1]!r})')
	if problems:
		print(f'{len(problems)} translated strings have format arguments that don\'t match the English:')
		for problem in problems:
			print('  ' + problem)
		return 1
	print('All translated strings match the English format arguments')
	return 0


if __name__ == '__main__':
	sys.exit(main())
