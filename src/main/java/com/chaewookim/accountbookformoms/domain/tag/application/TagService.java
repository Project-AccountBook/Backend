package com.chaewookim.accountbookformoms.domain.tag.application;

import com.chaewookim.accountbookformoms.domain.tag.dao.BoardTagRepository;
import com.chaewookim.accountbookformoms.domain.tag.dao.TagRepository;
import com.chaewookim.accountbookformoms.domain.tag.dto.response.TagResponse;
import com.chaewookim.accountbookformoms.domain.tag.entity.BoardTag;
import com.chaewookim.accountbookformoms.domain.tag.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final BoardTagRepository boardTagRepository;

    public List<TagResponse> listAll() {
        return tagRepository.findAll().stream().map(TagResponse::from).toList();
    }

    public List<String> tagsOfBoard(Long boardId) {
        List<BoardTag> links = boardTagRepository.findByBoardId(boardId);
        if (links.isEmpty()) return List.of();
        List<Long> tagIds = links.stream().map(BoardTag::getTagId).toList();
        Map<Long, String> byId = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        return tagIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    public Map<Long, List<String>> tagsByBoards(Collection<Long> boardIds) {
        if (boardIds.isEmpty()) return Map.of();
        List<BoardTag> links = boardTagRepository.findByBoardIdIn(boardIds);
        if (links.isEmpty()) return Map.of();
        Set<Long> tagIds = new HashSet<>();
        links.forEach(l -> tagIds.add(l.getTagId()));
        Map<Long, String> tagNameById = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        Map<Long, List<String>> result = new HashMap<>();
        links.forEach(l -> result
                .computeIfAbsent(l.getBoardId(), k -> new ArrayList<>())
                .add(tagNameById.get(l.getTagId())));
        return result;
    }

    @Transactional
    public void setTagsForBoard(Long boardId, List<String> tagNames) {
        boardTagRepository.deleteByBoardId(boardId);
        if (tagNames == null || tagNames.isEmpty()) return;
        List<String> cleaned = tagNames.stream()
                .map(s -> s == null ? null : s.trim())
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .toList();
        if (cleaned.isEmpty()) return;

        Map<String, Tag> existing = tagRepository.findByNameIn(cleaned).stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));
        for (String name : cleaned) {
            Tag tag = existing.computeIfAbsent(name,
                    n -> tagRepository.save(Tag.builder().name(n).build()));
            boardTagRepository.save(BoardTag.builder()
                    .boardId(boardId)
                    .tagId(tag.getId())
                    .build());
        }
    }

    public List<Long> boardIdsWithTag(String tagName) {
        return tagRepository.findByName(tagName)
                .map(tag -> boardTagRepository.findBoardIdsByTagId(tag.getId()))
                .orElse(List.of());
    }
}
