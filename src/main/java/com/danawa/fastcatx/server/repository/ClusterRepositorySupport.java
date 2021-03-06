package com.danawa.fastcatx.server.repository;

import com.danawa.fastcatx.server.entity.Cluster;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

@Repository
public class ClusterRepositorySupport extends QuerydslRepositorySupport {

    private final JPAQueryFactory queryFactory;

    public ClusterRepositorySupport(JPAQueryFactory queryFactory) {
        super(Cluster.class);
        this.queryFactory = queryFactory;
    }


}
