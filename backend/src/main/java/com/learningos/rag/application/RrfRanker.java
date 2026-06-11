package com.learningos.rag.application;

import com.learningos.rag.domain.KbDocChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.LinkedHashMap;
import java.util.Set;

public class RrfRanker {

    public List<KbDocChunk> fuse(List<List<KbDocChunk>> rankedBranches, int topK, int rrfK) {
        if (rankedBranches == null || rankedBranches.isEmpty() || topK <= 0) {
            return List.of();
        }
        int safeRrfK = Math.max(1, rrfK);
        Map<String, Double> scores = new HashMap<>();
        Map<String, Integer> bestRank = new HashMap<>();
        SequencedMap<String, KbDocChunk> chunksById = new LinkedHashMap<>();

        for (List<KbDocChunk> branch : rankedBranches) {
            if (branch == null || branch.isEmpty()) {
                continue;
            }
            Set<String> seenInBranch = new HashSet<>();
            int rank = 1;
            for (KbDocChunk chunk : branch) {
                if (chunk == null || chunk.getId() == null || !seenInBranch.add(chunk.getId())) {
                    rank++;
                    continue;
                }
                chunksById.putIfAbsent(chunk.getId(), chunk);
                scores.merge(chunk.getId(), 1.0d / (safeRrfK + rank), Double::sum);
                bestRank.merge(chunk.getId(), rank, Math::min);
                rank++;
            }
        }

        return chunksById.keySet().stream()
                .sorted((left, right) -> {
                    int byScore = Double.compare(scores.getOrDefault(right, 0.0d), scores.getOrDefault(left, 0.0d));
                    if (byScore != 0) {
                        return byScore;
                    }
                    int byRank = Integer.compare(bestRank.getOrDefault(left, Integer.MAX_VALUE),
                            bestRank.getOrDefault(right, Integer.MAX_VALUE));
                    if (byRank != 0) {
                        return byRank;
                    }
                    return left.compareTo(right);
                })
                .limit(Math.max(1, topK))
                .map(chunksById::get)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
