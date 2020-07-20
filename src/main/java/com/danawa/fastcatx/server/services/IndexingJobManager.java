package com.danawa.fastcatx.server.services;

import com.danawa.fastcatx.server.entity.IndexStep;
import com.danawa.fastcatx.server.entity.IndexingStatus;
import com.danawa.fastcatx.server.excpetions.IndexingJobFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IndexingJobManager {
    private static Logger logger = LoggerFactory.getLogger(IndexingJobManager.class);
    private final IndexingJobService indexingJobService;

    // KEY: collection id, value: Indexing status
    private ConcurrentHashMap<String, IndexingStatus> job = new ConcurrentHashMap<>();
    private static RestTemplate restTemplate;

    public IndexingJobManager(IndexingJobService indexingJobService) {
        this.indexingJobService = indexingJobService;
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10 * 1000);
        factory.setReadTimeout(10 * 1000);
        restTemplate = new RestTemplate(factory);
    }

    @Scheduled(cron = "*/5 * * * * *")
    private void fetchIndexingStatus() {
        if (job.size() == 0) {
            return;
        }
        Iterator<Map.Entry<String, IndexingStatus>> entryIterator = job.entrySet().iterator();
        entryIterator.forEachRemaining(entry -> {
            String id = entry.getKey();
            IndexingStatus indexingStatus = entry.getValue();
            try {
                URI url = URI.create(String.format("http://%s:%d/async/status?id=%s", indexingStatus.getHost(), indexingStatus.getPort(), indexingStatus.getIndexingJobId()));
                ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HashMap<>()), Map.class);
                String status = (String) responseEntity.getBody().get("status");
                logger.debug("{}", status);

                if ("SUCCESS".equalsIgnoreCase(status)) {
                    job.remove(id);
//                    TODO 다음 진행할 스텝이 있을 경우 호출하기.
                    IndexStep nextStep = indexingStatus.getNextStep().poll();
                    logger.debug("next Step >> {}", nextStep);
                } else if ("RUNNING".equalsIgnoreCase(status)) {

                } else if ("ERROR".equalsIgnoreCase(status)) {

                } else if ("READY".equalsIgnoreCase(status)) {

                }
            } catch (Exception e) {
                if (indexingStatus.getRetry() > 0) {
                    indexingStatus.setRetry(indexingStatus.getRetry() - 1);
                } else {
//                    TODO retry 하였지만 에러가 발생시.. 히스토리 추가 후 잡에서 제거
                    job.remove(id);
                }
            }
        });
    }


    public void add(String collectionId, IndexingStatus indexingStatus) throws IndexingJobFailureException {
        IndexingStatus registerIndexingStatus = findById(collectionId);
        if (registerIndexingStatus != null) {
            throw new IndexingJobFailureException("Duplicate Collection Indexing");
        }
        job.put(collectionId, indexingStatus);
    }

    public IndexingStatus findById(String collectionId) {
        return job.get(collectionId);
    }


}
