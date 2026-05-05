#1. Put all *.lproj files from iOS's Assets folder into the iosLocalizables folder in the root of this project
#2. run this script from the root of this project:
# python3 scripts/ios_localizable_to_android_xml.py


#!/usr/bin/env python3
# Script to convert iOS .strings and .stringsdict localizable files to Android XML
# Supports multiple language translations

import re
import xml.etree.ElementTree as ET
import sys
import os
from collections import defaultdict
import xml.dom.minidom as dom
from io import StringIO

# Input paths
ios_base_dir = "iosLocalizables"
android_base_dir = "app/src/main/res"

# iOS language folders pattern (e.g., en.lproj, bg.lproj, cs.lproj)
IOS_LPROJ_PATTERN = re.compile(r'^([a-z]{2}(?:_[A-Z]{2})?)\.lproj$')

# Map iOS language codes to Android resource folder names
# English (en) is special - it goes to 'values' folder (default)
LANGUAGE_CODE_MAP = {
	'zh-Hans': 'zh-rCN',  # Chinese Simplified
	'zh-Hant': 'zh-rTW',  # Chinese Traditional
	'pt-BR': 'pt-rBR',    # Portuguese Brazilian
	'pt-PT': 'pt',        # Portuguese Portugal
	'es-ES': 'es',        # Spanish Spain
	'en-US': 'en',        # English US
	'en-GB': 'en-rGB',    # English UK
}

# Android reserved keywords that cannot be used as resource names
ANDROID_RESERVED_KEYWORDS = {
	'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char',
	'class', 'const', 'continue', 'default', 'do', 'double', 'else', 'enum',
	'extends', 'final', 'finally', 'float', 'for', 'goto', 'if', 'implements',
	'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new',
	'package', 'private', 'protected', 'public', 'return', 'short', 'static',
	'strictfp', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws',
	'transient', 'try', 'void', 'volatile', 'while',
	# XML specific
	'xml', 'resources', 'string', 'plurals', 'item', 'quantity',
	# Additional reserved words
	'copy'
}

# Track used keys per language to detect conflicts
used_keys_per_lang = defaultdict(set)

def get_android_language_folder(lproj_folder):
	"""Convert iOS .lproj folder name to Android resource folder name"""
	lang_code = lproj_folder.replace('.lproj', '')

	# English is the default language, goes to 'values' folder
	if lang_code == 'en':
		return 'values'

	if lang_code in LANGUAGE_CODE_MAP:
		android_lang = LANGUAGE_CODE_MAP[lang_code]
	else:
		parts = lang_code.split('_')
		if len(parts) == 2:
			android_lang = f"{parts[0]}-r{parts[1].upper()}"
		else:
			android_lang = lang_code

	return f"values-{android_lang}"

def ensure_output_directory(dir_path):
	"""Create output directory if it doesn't exist"""
	if not os.path.exists(dir_path):
		os.makedirs(dir_path)
		print(f"Created output directory: {dir_path}")

def resolve_key_conflict(key, lang):
	"""Resolve conflicts with Android reserved keywords or duplicate keys for a specific language"""
	original_key = key
	counter = 1
	used_keys = used_keys_per_lang[lang]

	if key.lower() in ANDROID_RESERVED_KEYWORDS:
		key = f"{key}_1"

	while key in used_keys:
		key = f"{original_key}_{counter}"
		counter += 1

	used_keys.add(key)
	return key

def sanitize_string_name(key, lang):
	"""Sanitize string name for Android compatibility"""
	key = key.replace('.', '_')
	key = key.replace(' ', '_')
	key = re.sub(r'[^a-zA-Z0-9_.]', '_', key)

	if key and key[0].isdigit():
		key = "_" + key

	key = re.sub(r'_+', '_', key)
	key = key.rstrip('_')
	key = resolve_key_conflict(key, lang)

	return key

def escape_android_string(value):
	"""Escape special characters for Android XML string resources"""
	if not value:
		return value

	# Escape apostrophes
	value = value.replace("'", "\\'")

	# Escape quotes
	value = value.replace('"', '\\"')

	return value

def convert_ios_placeholder_to_android(value):
	"""Convert iOS placeholders to Android format with positional specifiers for multiple placeholders"""
	if not value:
		return value

	# First, check if there are already positional specifiers (like %1$@)
	if re.search(r'%\d+\$', value):
		def replace_positional(match):
			position = match.group(1)
			spec = match.group(2)
			if spec == '@':
				return f'%{position}$s'
			elif spec == 'd':
				return f'%{position}$d'
			elif spec == 'f':
				return f'%{position}$f'
			return match.group(0)

		value = re.sub(r'%(\d+)\$([@df])', replace_positional, value)
		return value

	# Find all non-positional placeholders
	placeholders = re.findall(r'%([@df])', value)

	if len(placeholders) <= 1:
		# Single placeholder, no need for positional
		value = re.sub(r'%([@df])', lambda m: '%s' if m.group(1) == '@' else ('%d' if m.group(1) == 'd' else '%f'), value)
		return value

	# Multiple placeholders, convert to positional format
	position = 1
	def replace_with_positional(match):
		nonlocal position
		spec = match.group(1)
		result = f'%{position}$'
		if spec == '@':
			result += 's'
		elif spec == 'd':
			result += 'd'
		elif spec == 'f':
			result += 'f'
		position += 1
		return result

	value = re.sub(r'%([@df])', replace_with_positional, value)
	return value

def parse_strings_file(filepath, lang, strings_resources, plurals_resources):
	"""Parse iOS .strings file and add entries to strings.xml"""
	if not os.path.exists(filepath):
		print(f"Warning: {filepath} not found, skipping...")
		return

	line_count = 0
	with open(filepath, "r", encoding="utf-8") as f:
		for line_num, line in enumerate(f, 1):
			line = line.strip()

			if not line or line.startswith("//") or line.startswith("/*"):
				continue

			match = re.match(r'^"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', line)
			if match:
				key, value = match.groups()

				# Unescape iOS escaped characters
				value = value.replace('\\"', '"')
				value = value.replace("\\'", "'")
				value = value.replace('\\\\', '\\')

				# Sanitize string name
				normalized_key = sanitize_string_name(key, lang)

				# Convert placeholders
				value = convert_ios_placeholder_to_android(value)

				# Escape special characters for Android XML
				value = escape_android_string(value)

				# Add as string element to strings.xml
				string_elem = ET.SubElement(strings_resources, "string", name=normalized_key)
				string_elem.text = value
				line_count += 1

	if line_count > 0:
		print(f"  Processed {line_count} strings from {filepath}")

def parse_dict_element(elem):
	"""Helper function to parse a dict element and return a dictionary"""
	result = {}
	children = list(elem)

	i = 0
	while i < len(children):
		if children[i].tag == 'key':
			key = children[i].text
			i += 1
			if i < len(children):
				value_elem = children[i]

				if value_elem.tag == 'string':
					result[key] = value_elem.text
				elif value_elem.tag == 'dict':
					result[key] = parse_dict_element(value_elem)
				elif value_elem.tag == 'array':
					array_values = [item.text for item in value_elem if item.tag == 'string']
					result[key] = array_values
		i += 1

	return result

def parse_stringsdict_plurals(element, strings_resources, plurals_resources, lang):
	"""Recursively parse stringsdict to find plural entries"""
	if element.tag == 'dict':
		children = list(element)

		for i, child in enumerate(children):
			if child.tag == 'key' and i + 1 < len(children):
				key_text = child.text
				next_child = children[i + 1]

				if next_child.tag == 'dict':
					next_dict_parsed = parse_dict_element(next_child)

					if 'NSStringLocalizedFormatKey' in next_dict_parsed:
						process_plural_entry(key_text, next_dict_parsed, strings_resources, plurals_resources, lang)
					else:
						parse_stringsdict_plurals(next_child, strings_resources, plurals_resources, lang)
				elif next_child.tag == 'array':
					pass
				else:
					parse_stringsdict_plurals(next_child, strings_resources, plurals_resources, lang)

	for child in element:
		parse_stringsdict_plurals(child, strings_resources, plurals_resources, lang)

def process_plural_entry(ios_key, plural_dict, strings_resources, plurals_resources, lang):
	"""Process a plural entry and add to Android resources"""
	format_key = plural_dict.get('NSStringLocalizedFormatKey', '')

	plural_match = re.search(r'%#@([^@]+)@', format_key)
	if plural_match:
		plural_var = plural_match.group(1)

		if plural_var in plural_dict:
			plural_rules = plural_dict[plural_var]

			if isinstance(plural_rules, dict):
				android_key = sanitize_string_name(ios_key, lang)

				plurals_elem = ET.SubElement(plurals_resources, "plurals", name=android_key)

				category_order = ['zero', 'one', 'two', 'few', 'many', 'other']

				for category in category_order:
					if category in plural_rules:
						value = plural_rules[category]
						value = value.replace('\\"', '"')
						value = value.replace("\\'", "'")
						value = value.replace('\\\\', '\\')

						value = re.sub(r'%#@[^@]+@', '%d', value)
						value = convert_ios_placeholder_to_android(value)
						value = escape_android_string(value)

						item_elem = ET.SubElement(plurals_elem, "item", quantity=category)
						item_elem.text = value

				base_format = re.sub(r'%#@[^@]+@', '%d', format_key)
				base_format = base_format.replace('\\"', '"')
				base_format = base_format.replace("\\'", "'")
				base_format = base_format.replace('\\\\', '\\')
				base_format = convert_ios_placeholder_to_android(base_format)
				base_format = escape_android_string(base_format)

				string_key = sanitize_string_name(f"{android_key}_format", lang)
				string_elem = ET.SubElement(strings_resources, "string", name=string_key)
				string_elem.text = base_format

def parse_stringsdict_file(filepath, lang, strings_resources, plurals_resources):
	"""Parse iOS .stringsdict file and convert plurals to plurals_strings.xml"""
	if not os.path.exists(filepath):
		print(f"Warning: {filepath} not found, skipping...")
		return

	try:
		tree = ET.parse(filepath)
		root = tree.getroot()
		parse_stringsdict_plurals(root, strings_resources, plurals_resources, lang)
	except ET.ParseError as e:
		print(f"Error parsing {filepath}: {e}")
	except Exception as e:
		print(f"Error processing {filepath}: {e}")

def write_xml_file(resources_element, filepath, filename):
	"""Helper function to write XML file with proper formatting - preserves single backslashes"""

	# Convert to string without any additional escaping
	rough_string = ET.tostring(resources_element, encoding='unicode', method='xml')

	# Parse with minidom and pretty print
	try:
		with StringIO(rough_string) as f:
			doc = dom.parse(f)

		# Pretty print with 4 spaces
		pretty_xml = doc.toprettyxml(indent="    ", encoding="utf-8").decode('utf-8')

		# Remove XML declaration and extra whitespace
		lines = pretty_xml.split('\n')
		result_lines = []
		in_xml_declaration = True

		for line in lines:
			if in_xml_declaration and line.strip().startswith('<?xml'):
				continue
			if in_xml_declaration and line.strip() == '':
				continue
			in_xml_declaration = False
			# Replace tabs with spaces
			line = line.replace('\t', '    ')
			result_lines.append(line)

		# Join and strip
		result = '\n'.join(result_lines).strip()

		# Write the file
		with open(filepath, "w", encoding="utf-8") as f:
			f.write('<?xml version="1.0" encoding="utf-8"?>\n')
			f.write('<!-- Auto-generated from iOS Localizable.strings and Localizable.stringsdict -->\n')
			f.write(result)
			f.write('\n')

	except Exception as e:
		# Fallback: write without pretty printing
		print(f"  Warning: Could not prettify XML, writing as-is")
		with open(filepath, "wb") as f:
			f.write(b'<?xml version="1.0" encoding="utf-8"?>\n')
			f.write(b'<!-- Auto-generated from iOS Localizable.strings and Localizable.stringsdict -->\n')
			ET.ElementTree(resources_element).write(f, encoding="utf-8", xml_declaration=False)

def process_language(lproj_folder, ios_path, android_res_path):
	"""Process a single language translation"""
	lang_code = lproj_folder.replace('.lproj', '')
	android_folder = get_android_language_folder(lproj_folder)
	android_output_dir = os.path.join(android_res_path, android_folder)

	print(f"\n  Processing {lang_code} ({android_folder})...")

	# Reset used keys for this language
	used_keys_per_lang[lang_code] = set()

	strings_resources = ET.Element("resources")
	plurals_resources = ET.Element("resources")

	strings_file = os.path.join(ios_path, "Localizable.strings")
	stringsdict_file = os.path.join(ios_path, "Localizable.stringsdict")

	parse_strings_file(strings_file, lang_code, strings_resources, plurals_resources)
	parse_stringsdict_file(stringsdict_file, lang_code, strings_resources, plurals_resources)

	ensure_output_directory(android_output_dir)

	strings_written = False
	plurals_written = False

	if len(strings_resources) > 0:
		strings_output = os.path.join(android_output_dir, "imported_strings.xml")
		write_xml_file(strings_resources, strings_output, "imported_strings.xml")
		strings_written = True

	if len(plurals_resources) > 0:
		plurals_output = os.path.join(android_output_dir, "imported_strings_plural.xml")
		write_xml_file(plurals_resources, plurals_output, "imported_strings_plural.xml")
		plurals_written = True

	if strings_written or plurals_written:
		print(f"    ✓ Generated files in {android_folder}/")
		if strings_written:
			print(f"      - imported_strings.xml")
		if plurals_written:
			print(f"      - imported_strings_plural.xml")
	else:
		print(f"    ⚠ No content found for {lang_code}")

def main():
	"""Main function to convert iOS localizable files to Android format"""

	print("=" * 70)
	print("iOS to Android Localization Converter")
	print("=" * 70)
	print(f"iOS base directory: {ios_base_dir}/")
	print(f"Android base directory: {android_base_dir}/")
	print("-" * 70)

	if not os.path.exists(ios_base_dir):
		print(f"Error: iOS base directory '{ios_base_dir}' not found!")
		sys.exit(1)

	lproj_folders = []
	for item in os.listdir(ios_base_dir):
		item_path = os.path.join(ios_base_dir, item)
		if os.path.isdir(item_path) and IOS_LPROJ_PATTERN.match(item):
			lproj_folders.append(item)

	if not lproj_folders:
		print(f"Error: No .lproj folders found in '{ios_base_dir}'!")
		sys.exit(1)

	print(f"\nFound {len(lproj_folders)} language folders:")
	for folder in lproj_folders:
		print(f"  • {folder}")

	print("\n" + "=" * 70)
	print("Processing translations...")
	print("=" * 70)

	# First, ensure English (en) is processed to create default 'values' folder
	en_folder = None
	other_folders = []

	for folder in lproj_folders:
		if folder == 'en.lproj':
			en_folder = folder
		else:
			other_folders.append(folder)

	# Process English first (will go to 'values' folder)
	if en_folder:
		ios_path = os.path.join(ios_base_dir, en_folder)
		process_language(en_folder, ios_path, android_base_dir)

	# Process other languages
	for lproj_folder in sorted(other_folders):
		ios_path = os.path.join(ios_base_dir, lproj_folder)
		process_language(lproj_folder, ios_path, android_base_dir)

	# If no English folder found, create empty default values folder
	if not en_folder:
		default_values_dir = os.path.join(android_base_dir, "values")
		if not os.path.exists(default_values_dir):
			ensure_output_directory(default_values_dir)
			empty_strings = ET.Element("resources")
			empty_plurals = ET.Element("resources")
			write_xml_file(empty_strings, os.path.join(default_values_dir, "imported_strings.xml"), "imported_strings.xml")
			write_xml_file(empty_plurals, os.path.join(default_values_dir, "imported_strings_plural.xml"), "imported_strings_plural.xml")
			print(f"\n  Created default values folder with empty files")

	print("\n" + "=" * 70)
	print("✅ Conversion complete!")
	print(f"Android resource files created in: {android_base_dir}/")
	print("=" * 70)

if __name__ == "__main__":
	main()