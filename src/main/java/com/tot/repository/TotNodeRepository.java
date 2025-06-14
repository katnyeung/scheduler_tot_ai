package com.tot.repository;


import com.tot.entity.TotNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TotNodeRepository extends JpaRepository<TotNode, String> {
    /**
     * Find all nodes belonging to a specific tree
     */
    List<TotNode> findByTreeId(String treeId);

    /**
     * Find a specific node by its nodeId and treeId
     */
    Optional<TotNode> findByNodeIdAndTreeId(String nodeId, String treeId);

    /**
     * Find leaf nodes (nodes with no children) in a tree
     */
    @Query("SELECT t FROM TotNode t WHERE t.treeId = :treeId AND t.nodeId NOT IN " +
            "(SELECT DISTINCT VALUE(c) FROM TotNode n JOIN n.children c WHERE n.treeId = :treeId)")
    List<TotNode> findLeafNodesByTreeId(@Param("treeId") String treeId);

    /**
     * Find nodes by content containing the given text (case insensitive)
     */
    List<TotNode> findByContentContainingIgnoreCase(String contentText);

    /**
     * Count nodes in a specific tree
     */
    long countByTreeId(String treeId);

    /**
     * Find all distinct tree IDs
     */
    @Query("SELECT DISTINCT t.treeId FROM TotNode t")
    List<String> findAllTreeIds();
}