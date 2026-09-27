package com.example.tca_app;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * ViewModel for HomeFeedFragment to observe posts via PostRepository.
 * Implements lifecycle-aware subscription management.
 */
public class PostViewModel extends ViewModel {
    private final PostRepository repository = new PostRepository();
    private final MutableLiveData<List<Post>> postsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<List<Post>> getPostsLiveData() {
        return postsLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    /**
     * Starts listening to posts for the given category.
     * Delegates to PostRepository which manages the Firestore listener.
     */
    public void startListening(String category) {
        repository.listenToFeedPosts(category, postsLiveData, errorLiveData);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removePostsListener();
    }
}
