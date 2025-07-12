xml_template = """<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="../../../../../documentation2html.xsl"?>
<p1:documents xmlns:p1="http://rapid-i.com/schemas/documentation/reference/1.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://rapid-i.com/schemas/documentation/reference/1.0                http://rapid-i.com/schemas/documentation/reference/1.0/documentation.xsd">

    <operator key="operator.{operator_key}" locale="en" version="7.6.000">

    <title>{operator_name}</title>
{description}
{differentiation}
{inputPorts}
{outputPorts}
{parameters}
{tutorial_processes}
    </operator>
</p1:documents>"""