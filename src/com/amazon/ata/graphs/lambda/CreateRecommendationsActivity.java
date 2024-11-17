package com.amazon.ata.graphs.lambda;

import com.amazon.ata.graphs.dynamodb.FollowEdge;
import com.amazon.ata.graphs.dynamodb.FollowEdgeDao;
import com.amazon.ata.graphs.dynamodb.Recommendation;
import com.amazon.ata.graphs.dynamodb.RecommendationDao;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.lambda.runtime.Context;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class CreateRecommendationsActivity {

    private FollowEdgeDao followEdgeDao;
    private RecommendationDao recommendationDao;

    public CreateRecommendationsActivity(RecommendationDao recommendationDao, FollowEdgeDao followEdgeDao) {
        this.followEdgeDao = followEdgeDao;
        this.recommendationDao = recommendationDao;
    }

    public List<Recommendation> handleRequest(CreateRecommendationsRequest input, Context context) {
        if (input == null|| input.getUsername() == null){
            throw new InvalidParameterException("missing input");
        }
        List<Recommendation> recommendations = new ArrayList<>();
        PaginatedQueryList <FollowEdge> followEdgePaginatedQueryList = this.followEdgeDao.getAllFollows(input.getUsername());
        List<String> follows = followEdgePaginatedQueryList.stream()
        .map(FollowEdge::getFromUsername)
                .collect(Collectors.toList());

        for (String username : follows){
            PaginatedQueryList<FollowEdge> followsFollowEdges = this.followEdgeDao.getAllFollows(username);
            List<String> followsFollows = followsFollowEdges.stream()
                    .map(FollowEdge::getFromUsername).collect(Collectors.toList());
            for(String followsFollow : followsFollows){
                if (followsFollow != input.getUsername() && !follows.contains(followsFollow)
                        && !recommendations.contains(followsFollow)){
                    recommendations.add(new Recommendation(input.getUsername(), followsFollow, "active"));
                    if (recommendations.size() >= input.getLimit()){
                        return recommendations;
                    }
                }
            }
        }

        return recommendations;
    }
}
