"""convert_to_xml

Usage:
    convert_to_xml.py [--buildpath=<buildpath>] [--all] [--mdpath=<mdpath>]

Options:
    -h --help           Help
    --mdpath=<path>     Path to the markdown file
    --buildpath=<path>  Path to store the xml file [default: ../../src/main/resources/streaming]
    --all               Converts all md files in subfolders


"""
from docopt import docopt
from collections import OrderedDict

import markdown as md
from xml_template import xml_template
import os
import re


def parse_port_type(type):
    if type.startswith("com."):
        return type
    if type == "Data Table":
        return "com.rapidminer.example.ExampleSet"
    if type == "Decision Tree":
        return "com.rapidminer.operator.learner.tree.TreeModel"
    if type == "Model":
        return "com.rapidminer.operator.Model"
    if type == "IOObject":
        return "com.rapidminer.operator.IOObject"
    if type == "Connection":
        return "com.rapidminer.connection.ConnectionInformationContainerIOObject"
    if type == "Stream Data Container":
        return "com.rapidminer.extension.streaming.ioobject.StreamDataContainer"
    raise Exception("port type (" + type +  ") not parseable, change it to something with com.* or add the corresponding if clause in parse_port_type(type).")

def convert_text_to_rapidminer_xml(text):
    text = text.replace('&', '&amp;')
    text = text.replace('>', '&gt;')
    text = text.replace('<', '&lt;')
    text = re.sub(r'\*\*([\&;,\<\.\>\w\s-]+)\*\*', r'<em>\1</em>', text)
    text = re.sub(r'\*([\(\)=_\&;,\<\.\>\w\s-]+)\*', r'<em>\1</em>', text)
    text = re.sub(r'\s_(\&;,\<\.\>[a-zA-Z0-9\s-]+)_\s', r' <em>\1</em> ', text)
    text = text.replace("\*","*")
    text = text.replace("\_", "_")

    is_list = re.search("^(\s*)-\s",text, re.MULTILINE)
    if is_list:
        intendation = is_list.group(1)
        list_item_start = is_list.group(0)
        list_items = []
        list_items_text = ""
        lines_of_paragraph = text.split("\n")
        for line in lines_of_paragraph:
            if line.startswith(list_item_start):
                # If we already have stored something in list_items_text we have to append this to the list_items
                if list_items_text != "":
                    # Add end of listitem xml code
                    list_items_text += intendation + "    </li>"
                    list_items.append(list_items_text)
                    # Reset list_items_text
                    list_items_text = ""
                # Start with new listitem xml code
                list_items_text += intendation + "    <li>\n"
            # Add the current line to the list_items_text
            list_items_text += intendation + "        " + line.replace(list_item_start,"").strip() + "\n"
        if list_items_text != "":
            # Add end of listitem xml code
            list_items_text += intendation + "    </li>"
            list_items.append(list_items_text)
        text = """{intend}<ul>
{list_items}
{intend}</ul>
""".format(intend=intendation, list_items = "\n".join(list_items))
    return text


def formatParagraph(p,intendation = "", stripLines = True):
    listOfLines = []
    if stripLines:
        p = p.strip()
    lines = p.split("\n")
    for line in lines:
        if stripLines:
            line = line.strip()
        listOfLines.append(intendation + line)
    return "\n".join(listOfLines)

def splitTitleAndBrackets(title):
    regularExpression = re.search("\(([\w:_\-\.\s]+)\)", title)
    if regularExpression != None:
        brackets = regularExpression.group(1)
        title = title.replace("(" + brackets + ")", "").strip()
    else:
        brackets = None
        title = title.strip()
    return title,brackets

def getRawDict(raw_markdown):
    raw_dict = OrderedDict()
    raw_dict["meta_data"] = OrderedDict()
    sectionTitle = ""
    subsectionTitle = ""
    section_indicator = "## "
    subsection_indicator = "#### "
    paragraphs = []
    text_of_one_paragraph = ""
    for line in raw_markdown:
        # obtaining meta data from file beginning
        if line.startswith('# '):
            raw_dict['meta_data']['operator_name'] = line.split('# ')[1].replace('\n', '')
        if line.startswith('Additional Tags:'):
            raw_dict['meta_data']['tags_added'] = line.split(':')[1].strip().replace('*', '').split(',')
        if line.startswith('Tags to be removed:'):
            raw_dict['meta_data']['tags_removed'] = line.split(':')[1].strip().replace('*', '').split(',')
        if line.startswith('Operator Key:'):
            raw_dict['meta_data']['operator_key'] = line.partition(':')[2].strip()
        if line.startswith('Group:'):
            raw_dict['meta_data']['group'] = line.partition(':')[2].strip()

        if line.startswith(section_indicator):
            # Store last paragraph if necesseary
            if text_of_one_paragraph != "":
                paragraphs.append(text_of_one_paragraph)
                text_of_one_paragraph = ""
            # Store paragraphs of last subsection
            if len(paragraphs) > 0 and sectionTitle != "":
                if subsectionTitle == "":
                    subsectionTitle = "default"
                raw_dict[sectionTitle][subsectionTitle] = paragraphs
                paragraphs = []
            # Start with new section
            sectionTitle = line.split(section_indicator)[1].replace('\n', '').strip()
            raw_dict[sectionTitle] = OrderedDict()
            subsectionTitle = ""
            paragraphs = []
            text_of_one_paragraph = ""
        elif line.startswith(subsection_indicator):
            # Store last paragraph if necesseary
            if text_of_one_paragraph != "":
                paragraphs.append(text_of_one_paragraph)
                text_of_one_paragraph = ""
            # Store paragraphs of last subsection
            if len(paragraphs) > 0 and sectionTitle != "":
                if subsectionTitle == "":
                    subsectionTitle = "default"
                raw_dict[sectionTitle][subsectionTitle] = paragraphs
                paragraphs = []
            # start new subection
            subsectionTitle = line.split(subsection_indicator)[1].replace('\n', '').strip()
            paragraphs = []
            text_of_one_paragraph = ""
        else:
            if line == "" or line == "\n":
                # An empty line is an indicator for a new paragraph. If we already stored something in text_of_one_paragraph
                # we append this to the paragraphs list and set text_of_one_paragraph back to an empty string
                if text_of_one_paragraph != "":
                    paragraphs.append(text_of_one_paragraph)
                    text_of_one_paragraph = ""
            else:
                text_of_one_paragraph += line
    # Store last paragraph if necesseary
    if text_of_one_paragraph != "":
        paragraphs.append(text_of_one_paragraph)
    # Store paragraphs of last subsection
    if subsectionTitle != "":
        raw_dict[sectionTitle][subsectionTitle] = paragraphs

    return raw_dict

def addDescriptionText(xml_dict,paragraphs):
    # Get first line for the synopsis
    firstLine = paragraphs[0].partition(".")[0].strip() + "."
    firstLine = convert_text_to_rapidminer_xml(firstLine)
    text = """
    <synopsis>
        {}
    </synopsis>
""".format(firstLine)
    # Remove firstline from description text
    paragraphs[0] = paragraphs[0].replace(firstLine,"")
    description_text = ""
    for p in paragraphs:
        if p.strip() != "":
            p = formatParagraph(p,"            ")
            description_text += """
        <paragraph>
{text}
        </paragraph>""".format(text=convert_text_to_rapidminer_xml(p))
    text += """
    <text>{description_text}
    </text>
""".format(description_text = description_text)
    xml_dict["description"] = text
    return xml_dict

def addDifferentiationSection(xml_dict,comparison_sub_sections):
    differentiation_text = ""
    for key in comparison_sub_sections:
        current_comparison_text = ""
        paragraphs = comparison_sub_sections[key]
        for paragraph in paragraphs:
            paragraph = formatParagraph(paragraph, "                ")
            current_comparison_text += """
            <paragraph>
{text}
            </paragraph>""".format(text=convert_text_to_rapidminer_xml(paragraph))
        if key != "default":
            title, key = splitTitleAndBrackets(key)
            differentiation_text += """        
        <relatedDocument key = "{key}">{text}        
        </relatedDocument>""".format(key=key,text=current_comparison_text)
        else:
            differentiation_text += """{text}""".format(text=current_comparison_text)
    text = """
    <differentiation>{text}
    </differentiation>
""".format(text = differentiation_text)
    xml_dict["differentiation"] = text
    return xml_dict

def addPorts(xml_dict,ports_key,ports_sub_section):
    ports_text = ""
    for key in ports_sub_section:
        paragraphs = ports_sub_section[key]
        current_port_text = ""
        for paragraph in paragraphs:
            paragraph = formatParagraph(paragraph, "                ")
            current_port_text += """
            <paragraph>
{text}
            </paragraph>""".format(text=convert_text_to_rapidminer_xml(paragraph))
        if key == "default":
            ports_text += current_port_text
        else:
            title, key = splitTitleAndBrackets(key)
            type = parse_port_type(key)
            ports_text += """
        <port name = "{title}" type = "{type}">{text}
        </port>""".format(title=title, type = type, text=current_port_text)
    text = """
    <{ports_key}>{text}
    </{ports_key}>
    """.format(ports_key = ports_key, text=ports_text)
    xml_dict[ports_key] = text
    return xml_dict

def getValueXMLString(paragraph):
    paragraph = paragraph.replace("- ", "",1)
    value_key = paragraph.split(":")[0].replace("**","")
    if value_key[-1] == ":":
        value_key = value_key[:-1]
    remaining_paragraph = paragraph.replace("**" + value_key + "**:","").replace("**" + value_key + ":**","")
    remaining_paragraph = convert_text_to_rapidminer_xml(formatParagraph(remaining_paragraph,"                    "))
    text = """                <value value = "{key}">
{text}
                </value>""".format(key = value_key, text = remaining_paragraph)
    return text

def addParameters(xml_dict,parameters_sub_section):
    parameters_text = ""
    for key in parameters_sub_section:
        title = key.replace("(optional)","")
        nounderscore = False
        if "(nounderscore)" in title:
            nounderscore = True
            title = title.replace("(nounderscore)","")
        #title,optional = splitTitleAndBrackets(key)
        paragraphs = parameters_sub_section[key]
        parameter_text = ""
        values = []
        for paragraph in paragraphs:
            # if the paragraph starts with '- ' we have the possible values
            if paragraph.startswith("- **"):
                values.append(getValueXMLString(paragraph))
            else:
                paragraph = formatParagraph(paragraph, "                ")
                parameter_text += """
            <paragraph>
{text}
            </paragraph>""".format(text=convert_text_to_rapidminer_xml(paragraph))
        if len(values) > 0:
            parameter_text += """
            <values>
{text}
            </values>""".format(text = "\n".join(values))
        parameter_key = title.strip()
        if nounderscore is False:
            parameter_key = parameter_key.replace(" ","_").strip()
        parameters_text += """
        <parameter key = "{key}" >{text}
        </parameter>""".format(key = parameter_key, text = parameter_text)
    text = """
    <parameters>{text}
    </parameters>""".format(text = parameters_text)
    xml_dict["parameters"] = text
    return xml_dict

def addTutorials(xml_dict, operator_key, tutorials_sub_section):
    tutorials_text = ""
    for key in tutorials_sub_section:
        tutorial_number, tutorial_name = splitTitleAndBrackets(key)
        operatorKeyString = operator_key.lower().replace(" ", "_").replace(":",".").replace("/","_")
        tutorialNameString = tutorial_name.lower().replace(" ", "_").replace("(","_").replace(")","_").replace("/","_")
        tutorial_key = "process." + operatorKeyString + "." + tutorialNameString
        paragraphs = tutorials_sub_section[key]
        description_text = ""
        xml_code = ""
        for paragraph in paragraphs:
            # if the paragraph starts with '- ' we have the possible values
            if paragraph.startswith("```xml"):
                paragraph = paragraph.replace("```xml","").replace("```","").replace('<?xml version="1.0" encoding="UTF-8"?>','').strip()
                xml_code = formatParagraph(paragraph,"            ",stripLines=False)
            else:
                paragraph = formatParagraph(paragraph, "                    ")
                description_text += """
                <paragraph>
{text}
                </paragraph>""".format(text=convert_text_to_rapidminer_xml(paragraph))

        tutorial_text = """
            <description>{text}
            </description>
{xml_code}""".format(text=description_text,xml_code = xml_code)
        tutorials_text += """
        <tutorialProcess key = "{key}" title = "{title}">{text}
        </tutorialProcess>""".format(key=tutorial_key, title = tutorial_name, text=tutorial_text)
    text = """
    <tutorialProcesses>{text}
    </tutorialProcesses>""".format(text=tutorials_text)
    xml_dict["tutorial_processes"] = text
    return xml_dict

def convertOneFile(mdpath, buildpath):
    if not os.path.isfile(mdpath):
        raise Exception("The specified file is not a valid file: " + mdpath)
    filename = os.path.basename(mdpath)
    if not filename.endswith(".md"):
        raise Exception("The specified file is not a markdown file: " + mdpath)
    with open(mdpath, "r", encoding="utf-8") as f:
        raw = f.readlines()

    raw_dict = getRawDict(raw)
    raw_dict_keys = raw_dict.keys()
    xml_dict = {}
    xml_dict["operator_name"] = raw_dict['meta_data']["operator_name"]
    xml_dict["operator_key"] = raw_dict['meta_data']["operator_key"]
    xml_dict = addDescriptionText(xml_dict, raw_dict["Description"]["default"])
    if "Comparison" in raw_dict_keys:
        xml_dict = addDifferentiationSection(xml_dict, raw_dict["Comparison"])
    else:
        xml_dict["differentiation"] = ""
    if "Input" in raw_dict_keys:
        xml_dict = addPorts(xml_dict, "inputPorts", raw_dict["Input"])
    else:
        xml_dict["inputPorts"] = ""
    if "Output" in raw_dict_keys:
        xml_dict = addPorts(xml_dict, "outputPorts", raw_dict["Output"])
    else:
        xml_dict["outputPorts"] = ""
    if "Parameters" in raw_dict_keys:
        xml_dict = addParameters(xml_dict, raw_dict["Parameters"])
    else:
        xml_dict["parameters"] = ""
    xml_dict = addTutorials(xml_dict, xml_dict["operator_key"], raw_dict["Tutorial Process"])

    xml_text = xml_template.format(**xml_dict)

    group = raw_dict['meta_data']["group"].lower()
    group_path = os.path.join(buildpath,*group.split("."))
    if not os.path.exists(group_path):
        os.mkdir(group_path)
    # xml_file_name = filename.replace(".md",".xml")
    xml_file_name = xml_dict["operator_key"].split(":")[-1] + ".xml"
    xml_outfile_path = os.path.join(group_path,xml_file_name)
    print(xml_outfile_path)
    with open(xml_outfile_path,"w", encoding="utf-8") as f:
        f.write(xml_text)

    return raw_dict["meta_data"]

def main(arguments):

    mdpath = arguments['--mdpath']
    buildpath = arguments['--buildpath']
    convert_all = arguments['--all']

    if not convert_all:
        convertOneFile(mdpath, buildpath)
    else:
        meta_data_dict = OrderedDict()
        for root, dirs, files in os.walk('./'):
            if root.startswith("./.git") or root.startswith("./__") or root == "./":
                continue
            for file in files:
                if file.endswith(".md"):
                    path = os.path.join(root,file)
                    print(path)
                    current_meta_data_dict = convertOneFile(path,buildpath)
                    meta_data_dict[current_meta_data_dict["operator_key"]] = current_meta_data_dict

        for key in meta_data_dict:
            added_tags = ", ".join(meta_data_dict[key]["tags_added"])
            removed_tags = ", ".join(meta_data_dict[key]["tags_removed"])
            if added_tags.strip() != "" or removed_tags.strip() != "":
                print("Operator: " + meta_data_dict[key]["operator_name"], "\t key: " + key)
                print("Tags to be added: " + added_tags)
                print("Tags to be removed: " + removed_tags)



if __name__ == '__main__':
    arguments = docopt(__doc__)
    main(arguments)


