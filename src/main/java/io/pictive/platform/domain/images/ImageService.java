package io.pictive.platform.domain.images;

import io.pictive.platform.domain.collections.Collection;
import io.pictive.platform.domain.search.IndexMaintainer;
import io.pictive.platform.domain.search.ImageSearcher;
import io.pictive.platform.domain.users.User;
import io.pictive.platform.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final LabelingService labelingService;
    private final TextExtractionService textExtractionService;
    private final IndexMaintainer indexMaintainer;
    private final ImageSearcher imageSearcher;

    private final PersistenceContext<User> userPersistenceContext;
    private final PersistenceContext<Image> imagePersistenceContext;
    private final PersistenceContext<Collection> collectionPersistenceContext;

    public List<Image> search(UUID ownerID, List<UUID> collectionIDs, List<String> labels, String text, String searchMode) {

        var owner = userPersistenceContext.find(ownerID);

        var collections = collectionPersistenceContext.findAll(collectionIDs);

        if (collections.stream().anyMatch(collection -> !collection.getSharedWith().contains(owner))) {
            throw new IllegalStateException(String.format("Unable to perform search: Given list of collections contains at least one collection user '%s' does not have access to.", ownerID));
        }

        var identifiedImageIDs = imageSearcher.identifyImageMatches(collections, labels, text, searchMode);
        return imagePersistenceContext.findAll(identifiedImageIDs);

    }

    public List<Image> create(UUID ownerID, List<String> base64Payloads) {

        var owner = userPersistenceContext.find(ownerID);

        var images = base64Payloads.stream()
                .map(Image::withProperties)
                .peek(image -> setImageToOwnerReference(image, owner))
                .peek(image -> setImageToDefaultCollectionReference(image, owner.getDefaultCollection()))
                .collect(Collectors.toList());
        labelingService.labelImages(images);
        textExtractionService.extractAndAddText(images);

        indexMaintainer.createOrUpdateCollectionIndexWithImages(owner.getDefaultCollection().getId(), images);

        imagePersistenceContext.persistAll(images);

        return images;

    }

    public List<Image> getForUserInCollection(UUID userID, UUID collectionID) {

        var user = userPersistenceContext.find(userID);

        var collection = collectionPersistenceContext.find(collectionID);

        if (!user.getSharedCollections().contains(collection)) {
            throw new IllegalStateException(String.format("Unable to retrieve images from collection: Collection '%s' was not shared with user '%s'.", collectionID, userID));
        }

        return new ArrayList<>(collection.getImages());

    }

    public List<Image> getAll() {

        return imagePersistenceContext.findAll();

    }

    private void setImageToOwnerReference(Image image, User owner) {

        image.setOwner(owner);
        owner.getOwnedImages().add(image);

    }

    private void setImageToDefaultCollectionReference(Image image, Collection defaultCollection) {

        image.getContainedInCollections().add(defaultCollection);
        defaultCollection.getImages().add(image);

    }


}
