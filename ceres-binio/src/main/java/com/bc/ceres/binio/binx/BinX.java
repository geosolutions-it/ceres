package com.bc.ceres.binio.binx;

import com.bc.ceres.binio.*;
import com.bc.ceres.binio.util.SequenceElementCountResolver;
import static com.bc.ceres.binio.util.TypeBuilder.COMP;
import static com.bc.ceres.binio.util.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.util.TypeBuilder.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;

/**
 * See the <a href="http://www.edikt.org/binx/">BinX Project</a>.
 */
public class BinX {
    private static final String VAR_SEQUENCE_COMPOUND_PREFIX = "VarSequenceCompound@";
    private static final String ANONYMOUS_COMPOUND_PREFIX = "AnonymousCompound@";

    private final URI uri;

    private Map<String, Type> parameters;
    private Map<String, Type> definitions;
    private CompoundType dataset;

    private Map<SequenceType, SequenceElementCountResolver> sequenceElementCountResolvers;
    private Map<String, SimpleType> primitiveTypes;

    private Namespace namespace;

    private static int anonymousCompoundId = 0;
    private static int varSequenceCompoundId = 0;

    public BinX(URI uri) throws IOException, BinXException {
        this.uri = uri;

        parameters = new HashMap<String, Type>();
        definitions = new HashMap<String, Type>();

        primitiveTypes = new HashMap<String, SimpleType>();
        primitiveTypes.put("byte-8", SimpleType.BYTE);
        primitiveTypes.put("unsignedByte-8", SimpleType.UBYTE);
        primitiveTypes.put("short-16", SimpleType.SHORT);
        primitiveTypes.put("unsignedShort-16", SimpleType.USHORT);
        primitiveTypes.put("integer-32", SimpleType.INT);
        primitiveTypes.put("unsignedInteger-32", SimpleType.UINT);
        primitiveTypes.put("integer-64", SimpleType.LONG);
        primitiveTypes.put("unsignedInteger-64", SimpleType.LONG); // care, we don't have an ULONG!
        primitiveTypes.put("float-32", SimpleType.FLOAT);
        primitiveTypes.put("float-64", SimpleType.DOUBLE);

        sequenceElementCountResolvers = new HashMap<SequenceType, SequenceElementCountResolver>();

        parseDocument();
    }


    public URI getURI() {
        return uri;
    }

    public String getNamespace() {
        return namespace.getURI();
    }

    public Map<String, Type> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public Map<String, Type> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public CompoundType getDataset() {
        return dataset;
    }

    Map<String, SimpleType> getPrimitiveTypes() {
        return Collections.unmodifiableMap(primitiveTypes);
    }

    private void parseDocument() throws IOException, BinXException {
        SAXBuilder builder = new SAXBuilder();
        Document document;
        try {
            document = builder.build(uri.toURL());
        } catch (JDOMException e) {
            throw new BinXException(MessageFormat.format("Failed to read ''{0}''", uri), e);
        }
        Element binxElement = document.getRootElement();
        namespace = binxElement.getNamespace();

        parseParameters(binxElement);
        parseDefinitions(binxElement);
        parseDataset(binxElement);
    }

    public Format getFormat(String formatName) throws BinXException {
        Format format = new Format(dataset);
        format.setName(formatName);
        for (Map.Entry<SequenceType, SequenceElementCountResolver> entry : sequenceElementCountResolvers.entrySet()) {
            format.addSequenceTypeMapper(entry.getKey(), entry.getValue());
        }
        return format;
    }

    private void parseParameters(Element binxElement) throws BinXException {
        Element parametersElement = getChild(binxElement, "parameters", false);
        if (parametersElement != null) {
            // todo - implement parameters
            throw new BinXException(MessageFormat.format("Element ''{0}'': Not implemented", parametersElement.getName()));
        }
    }

    private void parseDefinitions(Element binxElement) throws IOException, BinXException {
        Element definitionsElement = getChild(binxElement, "definitions", false);
        if (definitionsElement != null) {
            List defineTypeElements = getChildren(definitionsElement, "defineType", false);
            for (int i = 0; i < defineTypeElements.size(); i++) {
                Element defineTypeElement = (Element) defineTypeElements.get(i);
                String typeName = getAttributeValue(defineTypeElement, "typeName", true);
                Element child = getChild(defineTypeElement, true);
                Type type = parseNonSimpleType(child);
                if (type == null) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': ''{1}'' not expected here", defineTypeElement.getName(), child.getName()));
                }
                if (definitions.containsKey(typeName)) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Duplicate type definition ''{1}''", definitionsElement.getName(), typeName));
                }
                definitions.put(typeName, type);
            }
        }
    }

    private void parseDataset(Element binxElement) throws BinXException, IOException {
        Element datasetElement = getChild(binxElement, "dataset", true);
        List children = getChildren(datasetElement, true);
        ArrayList<CompoundType.Member> members = new ArrayList<CompoundType.Member>(children.size());
        for (int i = 0; i < children.size(); i++) {
            Element element = (Element) children.get(i);
            String memberName = getAttributeValue(element, "varName", true);
            Type memberType = parseAnyType(element);
            members.add(MEMBER(memberName, memberType));
        }
        if (members.size() == 1 && members.get(0).getType() instanceof CompoundType) {
            CompoundType.Member member = members.get(0);
            dataset = COMP(member.getName(), ((CompoundType) member.getType()).getMembers());
        } else {
            dataset = COMP("_dataset", members.toArray(new CompoundType.Member[members.size()]));
        }
    }

    private Type parseNonSimpleType(Element typeElement) throws BinXException {
        String childName = typeElement.getName();
        Type type = null;
        if (childName.equals("struct")) {
            type = parseStruct(typeElement);
        } else if (childName.equals("union")) {
            type = parseUnion(typeElement);
        } else if (childName.equals("arrayFixed")) {
            type = parseArrayFixed(typeElement);
        } else if (childName.equals("arrayStreamed")) {
            type = parseArrayStreamed(typeElement);
        } else if (childName.equals("arrayVariable")) {
            type = parseArrayVariable(typeElement);
        }
        return type;
    }

    private Type parseAnyType(Element typeElement) throws BinXException {
        String typeName = typeElement.getName();
        Type type;
        if (typeName.equals("useType")) {
            type = parseUseType(typeElement);
        } else {
            type = parseNonSimpleType(typeElement);
            if (type == null) {
                type = primitiveTypes.get(typeElement.getName());
                if (type == null) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Unknown type: {1}", typeElement.getName(), typeName));
                }
            }
        }
        return type;
    }

    //    <useType typeName="Confidence_Descriptors_Data_Type" varName="Confidence_Descriptors_Data"/>
    //
    private Type parseUseType(Element typeElement) throws BinXException {
        String typeName = getAttributeValue(typeElement, "typeName", true);
        Type type = definitions.get(typeName);
        if (type == null) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': Unknown type definition: {1}", typeElement.getName(), typeName));
        }
        return type;
    }

    //    <struct>
    //        <integer-32 varName="Days"/>
    //        <unsignedInteger-32 varName="Seconds"/>
    //        <unsignedInteger-32 varName="Microseconds"/>
    //    </struct>
    //
    private Type parseStruct(Element typeElement) throws BinXException {
        List memberElements = getChildren(typeElement, false);
        ArrayList<CompoundType.Member> members = new ArrayList<CompoundType.Member>();
        for (int i = 0; i < memberElements.size(); i++) {
            Element memberElement = (Element) memberElements.get(i);
            String memberName = getAttributeValue(memberElement, "varName", true);
            Type memberType = parseAnyType(memberElement);
            // inline variable-length arrays
            if (memberType instanceof CompoundType) {
                CompoundType compoundType = (CompoundType) memberType;
                if (compoundType.getName().startsWith(VAR_SEQUENCE_COMPOUND_PREFIX)) {
                    Type counterType = compoundType.getMember(0).getType();
                    Type listType = compoundType.getMember(1).getType();
                    members.add(MEMBER(memberName + "_Count", counterType));
                    members.add(MEMBER(memberName + "_List", listType));
                    sequenceElementCountResolvers.put((SequenceType) listType, new VariableArrayResolver(i));
                } else {
                    members.add(MEMBER(memberName, memberType));
                }
            } else {
                members.add(MEMBER(memberName, memberType));
            }
        }
        return COMP(generateCoumpoundName(), members.toArray(new CompoundType.Member[members.size()]));
    }

    //    <arrayVariable varName="SM_SWATH" byteOrder="littleEndian">
    //        <sizeRef>
    //            <unsignedInteger-32 varName="N_Grid_Points"/>
    //        </sizeRef>
    //        <useType typeName="Grid_Point_Data_Type"/>
    //        <dim/>
    //    </arrayVariable>
    //
    private Type parseArrayVariable(Element typeElement) throws BinXException {
        Element sizeRefElement = getChild(typeElement, "sizeRef", 0, true);
        Element arrayTypeElement = getChild(typeElement, 1, true);
        Element dimElement = getChild(typeElement, "dim", 2, true);

        Element sizeRefTypeElement = getChild(sizeRefElement, true);
        String sizeRefName = getAttributeValue(sizeRefTypeElement, "varName", true);
        Type sizeRefType = parseAnyType(sizeRefTypeElement); // must be integer!

        Type arrayType = parseAnyType(arrayTypeElement);
        SequenceType dimType = SEQ(arrayType);

        String dimName = getAttributeValue(dimElement, "name", false);
        if (dimName == null) {
            dimName = "Data";
        }

        return COMP(generateVarSequenceCompoundName(),
                    MEMBER(sizeRefName, sizeRefType),
                    MEMBER(dimName, dimType));
    }

    private Type parseUnion(Element typeElement) throws BinXException {
        // todo - implement union
        throw new BinXException(MessageFormat.format("Element ''{0}'': Type not implemented", typeElement.getName()));
    }

    private Type parseArrayFixed(Element typeElement) throws BinXException {
        // todo - implement arrayFixed
        throw new BinXException(MessageFormat.format("Element ''{0}'': Type not implemented", typeElement.getName()));
    }

    private Type parseArrayStreamed(Element typeElement) throws BinXException {
        // todo - implement arrayStreamed
        throw new BinXException(MessageFormat.format("Element ''{0}'': Type not implemented", typeElement.getName()));
    }

    private String getAttributeValue(Element element, String name, boolean require) throws BinXException {
        String value = element.getAttributeValue(name);
        if (require && value == null) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': attribute ''{1}'' not found.", element.getName(), name));
        }
        return value;
    }

    private Element getChild(Element element, boolean require) throws BinXException {
        return getChild(element, 0, require);
    }

    private Element getChild(Element element, int index, boolean require) throws BinXException {
        return getChild(element, null, index, require);
    }

    private Element getChild(Element element, String name, boolean require) throws BinXException {
        Element child = element.getChild(name, namespace);
        if (require && child == null) {
            throw new BinXException(MessageFormat.format("Element ''{0}}': child ''{1}'' not found.", element.getName(), name));
        }
        return child;
    }

    private Element getChild(Element element, String name, int index, boolean require) throws BinXException {
        List children = getChildren(element, null, require);
        if (children.size() <= index) {
            if (require) {
                if (name != null) {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have a child ''{1}'' at index {2}", element.getName(), name, index));
                } else {
                    throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have a child at index {1}", element.getName(), index));
                }
            } else {
                return null;
            }
        }
        Element child = (Element) children.get(index);
        if (name != null && !name.equals(child.getName())) {
            throw new BinXException(MessageFormat.format("Element ''{0}'': Expected child ''{1}'' at index {2}", element.getName(), name, index));
        }
        return child;
    }

    private List getChildren(Element element, boolean require) throws BinXException {
        return getChildren(element, null, require);
    }

    private List getChildren(Element element, String name, boolean require) throws BinXException {
        List children = element.getChildren(name, namespace);
        if (require && children.isEmpty()) {
            if (name != null) {
                throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have at least one child of ''{1}''", element.getName(), name));
            } else {
                throw new BinXException(MessageFormat.format("Element ''{0}'': Expected to have at least one child", element.getName()));
            }
        }
        return children;
    }

    private String generateCoumpoundName() {
        return ANONYMOUS_COMPOUND_PREFIX + anonymousCompoundId++;
    }
    private String generateVarSequenceCompoundName() {
        return VAR_SEQUENCE_COMPOUND_PREFIX + varSequenceCompoundId++;
    }

    private static class VariableArrayResolver extends SequenceElementCountResolver {
        final int index;

        private VariableArrayResolver(int index) {
            this.index = index;
        }



        @Override
        public int getElementCount(CollectionData parent, SequenceType unresolvedSequenceType) throws IOException {
            // in BinX, variable arrays always have their length element at position zero 
            return parent.getInt(index);
        }
    }
}