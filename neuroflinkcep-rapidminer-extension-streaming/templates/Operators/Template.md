<!--Please delete all notes from this template in your Operator Markdown file: -->

<!-- Please write one Sentence per line (Pressing shift+enter for a new line in normal typora mode). If you want to include paragraphs use an empty line (Pressing enter in normal typora mode, in Source code mode there will be an emtpy line.) -->

<!-- You can include lists in every text paragraph by using the normal markdown lists. Be aware that there shouldn't be an empty line between the individual list items -->

<!-- There are some often occuring text snippets. You can find them in "Default Text.md". Use them if applicable, to ensure consistence. -->


# Operator Name

Additional Tags: 

Tags to be removed:

Operator Key: <!-- use the key from the xml process file in RM Studio (under class = <Operator Key>) ) -->

## Description

<!-- Short description of the Operator. The first sentence (until the first '.') will be taken as Synopsis -->

## Tutorial Process

#### Tutorial 1 (Name of Tutorial Process)

Description of tutorial process

```xml

```

## Comparison

General Text and/or single Operators

#### Name of Comparison Operator (key of Comparison Operator)

<!-- You have to specify every key for every Comparison Operator. Use the key from the xml process file in RM Studio (under class = <Operator Key>). The Name of Comparison Operator in the Markdown file will be ignored (the key is resolved by RM itself) -->

## Parameters

<!-- You have to correctly spell the parameter name including the correct capitalization. This will link to the key specified in the source code. Blanks are replaced by underscores by the python script. Write the name in the same way as it is spelled in the current help text. -->

<!-- You don't need to specify if its optional or any other meta data of the parameter. -->

#### parameter name (optional)

<!-- If the parameter has several possible values they are always displayed in a list with the possible values in bold. Be aware that this is not solved by a simple list in the xml. Hence if you have such a values list, please apply the following instructions: -->

<!-- Use markdown lists (line starting with '- ') to list the values. Between every value description there has to be an empty line. The value itself has to be the first of the list item and has to be in bold. -->

<!-- This means in typora source code mode each value has to look this way: '- **<value_key>**:' or '- **<value_key>:**' -->

<!-- You can split the description of one value over several lines, as long as you don't insert an empty line. Don't insert a normal paragraph inside the list. -->


## Input

#### port name (Object type)

<!-- This comment applies to all ports (Input and Output Port): -->

<!-- The Object type has to be in general the path to the class (so something like 'com.rapidminer.example.ExampleSet'). Currently the convert python script parse the following Objects type to the corresponding classes: Data Table, Decision Tree, Model, IOObject. If your Object type is not in this list specify the path type (you can look in the original xml file for example). Everything with starts with 'com.' is accepted by the script -->


## Output

#### port name (Object type)



