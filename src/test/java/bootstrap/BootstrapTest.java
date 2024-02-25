package bootstrap;

import org.junit.Test;

import domain.Post;

public class BootstrapTest extends AbstractJPAProgrammaticBootstrapTest {
	

	@Override
	protected Class<?>[] entities() {
		return new Class[] { Post.class };
	}
	
	@Test
	public void bootstrap() {
		this.doInJPA(entityManager -> {
            for (long id = 1; id <= 3; id++) {
                Post post = new Post();
                post.setId(id);
                post.setTitle(
                    String.format(
                        "High-Performance Java Persistence, Part %d", id
                    )
                );
                entityManager.persist(post);
            }
		});
	}
}
