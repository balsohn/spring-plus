package org.example.expert.domain.log.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QManagerLog is a Querydsl query type for ManagerLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QManagerLog extends EntityPathBase<ManagerLog> {

    private static final long serialVersionUID = 1713344580L;

    public static final QManagerLog managerLog = new QManagerLog("managerLog");

    public final org.example.expert.domain.common.entity.QTimestamped _super = new org.example.expert.domain.common.entity.QTimestamped(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath errorMessage = createString("errorMessage");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath requestType = createString("requestType");

    public final NumberPath<Long> requestUserId = createNumber("requestUserId", Long.class);

    public final StringPath status = createString("status");

    public final NumberPath<Long> targetManagerUserId = createNumber("targetManagerUserId", Long.class);

    public final NumberPath<Long> todoId = createNumber("todoId", Long.class);

    public QManagerLog(String variable) {
        super(ManagerLog.class, forVariable(variable));
    }

    public QManagerLog(Path<? extends ManagerLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QManagerLog(PathMetadata metadata) {
        super(ManagerLog.class, metadata);
    }

}

