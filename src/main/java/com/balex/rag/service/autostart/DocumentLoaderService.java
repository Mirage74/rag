package com.balex.rag.service.autostart;

import com.balex.rag.model.LoadedDocument;
import com.balex.rag.repo.DocumentRepository;
import lombok.SneakyThrows;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class DocumentLoaderService implements CommandLineRunner {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ResourcePatternResolver resolver;

    @Autowired
    private VectorStore vectorStore;


    @SneakyThrows
    public void loadDocuments() {
        List<Resource> resources = Arrays.stream(resolver.getResources("classpath:/knowledgebase/**/*.txt")).toList();

        resources.stream()
                .map(r -> Pair.of(r, calcContentHash(r)))
                .filter(p -> !documentRepository.existsByFilenameAndContentHash(p.getFirst().getFilename(), p.getSecond()))
                .forEach(p -> {
                    Resource resource = p.getFirst();
                    List<Document> docs = new TextReader(resource).get();
                    TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(200).build();
                    List<Document> chunks = splitter.apply(docs);

                    for (Document c : chunks) {
                        acceptWithRetry(vectorStore, List.of(c), 3, 1500);
                    }

                    LoadedDocument loaded = LoadedDocument.builder()
                            .documentType("txt")
                            .chunkCount(chunks.size())
                            .filename(resource.getFilename())
                            .contentHash(p.getSecond())
                            .build();
                    documentRepository.save(loaded);
                });

    }

    private static void acceptWithRetry(VectorStore vs, List<Document> part, int attempts, long sleepMs) {
        RuntimeException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                vs.accept(part);
                return;
            } catch (RuntimeException e) {
                last = e;
                try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
            }
        }
        throw last;
    }

    @SneakyThrows
    private String calcContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }

    @Override
    public void run(String... args) throws Exception {
        loadDocuments();
    }
}
