package de.caluga.morphium.objectmapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import de.caluga.morphium.*;
import de.caluga.morphium.annotations.Embedded;
import de.caluga.morphium.annotations.Entity;
import de.caluga.morphium.annotations.Reference;
import de.caluga.morphium.annotations.UseIfnull;
import de.caluga.morphium.driver.MorphiumId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MorphiumSerializer {

    private final List<Class> mongoTypes;
    private final AnnotationAndReflectionHelper anhelper;
    private final Map<Class<?>, NameProvider> nameProviderByClass;
    private final Morphium morphium;
    private final MorphiumObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(MorphiumSerializer.class);
    private final com.fasterxml.jackson.databind.ObjectMapper jackson;
    private final SimpleModule module;

    public MorphiumSerializer(AnnotationAndReflectionHelper ar, Map<Class<?>, NameProvider> np, Morphium m, MorphiumObjectMapper om) {
        mongoTypes = Collections.synchronizedList(new ArrayList<>());
        anhelper = ar;
        nameProviderByClass = np;
        morphium = m;

        objectMapper = om;
        module = new SimpleModule();


        module.addSerializer(MorphiumId.class, new JsonSerializer<MorphiumId>() {
            @Override
            public void serialize(MorphiumId morphiumId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeString("ObjectId(" + morphiumId.toString() + ")");
            }
        });

//        ScanResult res = new FastClasspathScanner("").scan();
//
//        for (String n:res.getNamesOfClassesWithAnnotation(Entity.class)){
//            log.info("Found Entity: "+n);
//            Class<?> cls = res.classNameToClassRef(n);
//
//        }
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
                if (anhelper.isAnnotationPresentInHierarchy(beanDesc.getBeanClass(), Entity.class) || anhelper.isAnnotationPresentInHierarchy(beanDesc.getBeanClass(), Embedded.class)) {
                    return new EntitySerializer((JsonSerializer<Object>) serializer, anhelper);
                }
                return serializer;
            }
        });
        jackson = new com.fasterxml.jackson.databind.ObjectMapper();
        jackson.registerModule(module);
    }


    public Map<String, Object> serialize(Object o) {
        try {
            jackson.writeValue(System.out, o);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map m = jackson.convertValue(o, Map.class);

        return m;
    }


    public <T> void registerTypeMapper(Class<T> cls, JsonSerializer<T> s) {
        module.addSerializer(cls, s);
    }

    public <T> void deregisterTypeMapperFor(Class<T> cls) {
        module.addSerializer(cls, null);
    }

    private NameProvider getNameProviderForClass(Class<?> cls, Entity p) throws IllegalAccessException, InstantiationException {
        if (p == null) {
            throw new IllegalArgumentException("No Entity " + cls.getSimpleName());
        }

        if (nameProviderByClass.get(cls) == null) {
            NameProvider np = p.nameProvider().newInstance();
            objectMapper.setNameProviderForClass(cls, np);
        }
        return nameProviderByClass.get(cls);
    }


    public String getCollectionName(Class cls) {
        Entity p = anhelper.getAnnotationFromHierarchy(cls, Entity.class); //(Entity) cls.getAnnotation(Entity.class);
        if (p == null) {
            throw new IllegalArgumentException("No Entity " + cls.getSimpleName());
        }
        try {
            cls = anhelper.getRealClass(cls);
            NameProvider np = getNameProviderForClass(cls, p);
            return np.getCollectionName(cls, objectMapper, p.translateCamelCase(), p.useFQN(), p.collectionName().equals(".") ? null : p.collectionName(), morphium);
        } catch (InstantiationException e) {
            log.error("Could not instanciate NameProvider: " + p.nameProvider().getName(), e);
            throw new RuntimeException("Could not Instaciate NameProvider", e);
        } catch (IllegalAccessException e) {
            log.error("Illegal Access during instanciation of NameProvider: " + p.nameProvider().getName(), e);
            throw new RuntimeException("Illegal Access during instanciation", e);
        }
    }



    public class EntitySerializer extends JsonSerializer<Object> {
        private final AnnotationAndReflectionHelper an;
        private JsonSerializer<Object> def;

        public EntitySerializer(JsonSerializer<Object> def, AnnotationAndReflectionHelper an) {
            this.def = def;
            this.an = an;
        }

        @Override
        public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();

            for (Field fld : an.getAllFields(o.getClass())) {
                try {
                    fld.setAccessible(true);
                    Object value = fld.get(o);
                    Reference r = fld.getAnnotation(Reference.class);
                    if (r != null && value != null) {
                        //create reference
                        Object id = anhelper.getId(value);
                        if (id == null && r.automaticStore()) {
                            morphium.storeNoCache(value);
                            id = anhelper.getId(value);
                        }
                        MorphiumReference ref = new MorphiumReference(value.getClass().getName(), id);
                        ref.setCollectionName(getCollectionName(value.getClass()));
                        value = ref;
                    }

                    UseIfnull un = fld.getAnnotation(UseIfnull.class);
                    if (value == null && un != null || value != null) {
                        String fldName = an.getFieldName(o.getClass(), fld.getName());
                        jsonGenerator.writeObjectField(fldName, value);
                        continue;
                    }

                } catch (IllegalAccessException e) {

                    e.printStackTrace();
                }
            }
            jsonGenerator.writeEndObject();
        }

    }
}
