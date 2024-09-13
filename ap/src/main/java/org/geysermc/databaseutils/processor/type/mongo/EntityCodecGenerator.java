/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.mongo;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.bson.BsonReader;
import org.bson.BsonSerializationException;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.util.TypedMap;

public final class EntityCodecGenerator {
    private EntityCodecGenerator() {}

    public static String createFor(EntityInfo info, TypeSpec.Builder typeSpec) {
        var typeName = info.type().getSimpleName() + "Codec";
        var codecType = TypeSpec.classBuilder(typeName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(Codec.class), ClassName.get(info.type())));

        codecType
                .addMethod(createConstructor(info, codecType))
                .addMethod(createDecode(info))
                .addMethod(createEncode(info))
                .addMethod(createGetEncoderClass(info))
                .build();

        typeSpec.addType(codecType.build());
        return typeName;
    }

    private static MethodSpec createConstructor(EntityInfo info, TypeSpec.Builder typeSpec) {
        var method = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(CodecRegistry.class), "registry");

        for (ColumnInfo column : info.columns()) {
            if (MongoTypeMappingRegistry.mongoTypeFor(column.typeName()) != null) {
                continue;
            }
            var fieldType = ParameterizedTypeName.get(ClassName.get(Codec.class), ClassName.get(column.asType()));
            typeSpec.addField(fieldType, column.name().toString(), Modifier.PRIVATE, Modifier.FINAL);
            method.addStatement("this.$L = registry.get($T.class)", column.name(), column.asType());
        }

        return method.build();
    }

    private static MethodSpec createDecode(EntityInfo info) {
        var method = MethodSpec.methodBuilder("decode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(info.asType()))
                .addParameter(ClassName.get(BsonReader.class), "reader")
                .addParameter(ClassName.get(DecoderContext.class), "context");

        method.addStatement("reader.readStartDocument()");

        method.addStatement("var map = new $T()", TypedMap.class);
        method.beginControlFlow("while (reader.readBsonType() != $T.END_OF_DOCUMENT)", BsonType.class);
        method.addStatement("var name = reader.readName()");

        for (ColumnInfo column : info.columns()) {
            method.beginControlFlow("if ($S.equals(name))", column.name());
            var type = MongoTypeMappingRegistry.mongoTypeFor(column.typeName());
            if (type == null) {
                method.addStatement("map.put($S, this.$L.decode(reader, context))", column.name(), column.name());
            } else {
                method.addStatement("map.put($S, reader.read$L())", column.name(), type);
            }
            method.addStatement("continue");
            method.endControlFlow();
        }

        // todo not just ignore _id
        method.beginControlFlow("if (!$S.equals(name))", "_id");
        method.addStatement("throw new $T($S.formatted(name))", BsonSerializationException.class, "Unknown field %s");
        method.endControlFlow();
        method.addStatement("reader.readObjectId()");

        method.endControlFlow();
        method.addStatement("reader.readEndDocument()");

        var mapGets = info.columns().stream()
                .map(column -> "map.get(\"%s\")".formatted(column.name()))
                .collect(Collectors.joining(", "));

        method.addStatement("return new $T($L)", info.type(), mapGets);
        return method.build();
    }

    private static MethodSpec createEncode(EntityInfo info) {
        var method = MethodSpec.methodBuilder("encode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(BsonWriter.class), "writer")
                .addParameter(ClassName.get(info.type()), "value")
                .addParameter(ClassName.get(EncoderContext.class), "context");

        method.addStatement("writer.writeStartDocument()");

        for (ColumnInfo column : info.columns()) {
            method.addStatement("writer.writeName($S)", column.name());
            var type = MongoTypeMappingRegistry.mongoTypeFor(column.typeName());
            if (type == null) {
                method.addStatement("this.$L.encode(writer, value.$L(), context)", column.name(), column.name());
            } else {
                method.addStatement("writer.write$L(value.$L())", type, column.name());
            }
        }

        method.addStatement("writer.writeEndDocument()");

        return method.build();
    }

    private static MethodSpec createGetEncoderClass(EntityInfo info) {
        return MethodSpec.methodBuilder("getEncoderClass")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), ClassName.get(info.type())))
                .addStatement("return $T.class", info.type())
                .build();
    }
}
