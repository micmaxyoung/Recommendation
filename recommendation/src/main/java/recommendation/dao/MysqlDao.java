package recommendation.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import recommendation.entity.Item;
import recommendation.entity.Item.ItemBuilder;
import recommendation.hibernate.CategoryId;
import recommendation.hibernate.HistoryId;
import recommendation.hibernate.PersistentCategories;
import recommendation.hibernate.PersistentHistory;
import recommendation.hibernate.PersistentItem;

@Repository
public class MysqlDao extends AbstractDaoImplementation {
	@Autowired
	@Qualifier("sessionFactory")
	private SessionFactory sessionFactory;

	@Override
	public void saveItem(Item item) {
		Session session = null;
		PersistentItem persistentItem = convert(item);

		try {
			session = sessionFactory.openSession();
			session.beginTransaction();
			session.save(persistentItem);
			session.getTransaction().commit();
			System.out.println("insert into db successfully");
		} catch (Exception ex) {
			if (session != null && session.getTransaction() != null) {
				session.getTransaction().rollback();
			}

			if (ex instanceof PersistenceException) {
				System.out.println("item id " + item.getItemId() + " already exists");
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
	
	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {

		Session session = null;

		for (String itemId : itemIds) {
			PersistentHistory persistentHistory = new PersistentHistory();
			persistentHistory.setHistoryId(new HistoryId(itemId, userId));
			try {
				session = sessionFactory.openSession();
				session.beginTransaction();
				session.save(persistentHistory);
				session.getTransaction().commit();
			} catch (Exception e) {
				if (session != null && session.getTransaction() != null) {
					session.getTransaction().rollback();
				}
				System.out.println("error insert history,  cause = " + e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {

		Session session = null;
		try {
			session = sessionFactory.openSession();
			session.beginTransaction();
			for (String itemId : itemIds) {
				PersistentHistory persistentHistory = new PersistentHistory();
				persistentHistory.setHistoryId(new HistoryId(itemId, userId));
				session.delete(persistentHistory);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	@Override
	public Set<String> getCategories(String itemId) {

		Set<String> categories = new HashSet<>();
		Session session = null;

		try {
			session = sessionFactory.openSession();
			session.beginTransaction();

			PersistentItem persistentItem = (PersistentItem) session.createCriteria(PersistentItem.class)
					.add(Restrictions.eq("itemId", itemId)).uniqueResult();

			List<PersistentCategories> categoriesList = session.createCriteria(PersistentCategories.class)
					.add(Restrictions.eq("persistentItem", persistentItem)).list();

			for (PersistentCategories category : categoriesList) {
				categories.add(category.getCategory());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return categories;
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {

		Set<String> favoriteItems = new HashSet<>();

		Session session = null;

		try {
			session = sessionFactory.openSession();
			session.beginTransaction();
			List<PersistentHistory> persistentHistories = session.createCriteria(PersistentHistory.class)
					.add(Restrictions.eq("historyId.userId", userId)).list();

			for (PersistentHistory history : persistentHistories) {
				favoriteItems.add(history.getHistoryId().getItemId());
			}

		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (session != null) {
				session.close();
			}
		}

		return favoriteItems;
	}

	public Set<Item> getFavoriteItems(String userId) {

		Set<String> itemIds = getFavoriteItemIds(userId);
		Set<Item> favoriteItems = new HashSet<>();

		Session session = null;
		try {

			session = sessionFactory.openSession();
			session.beginTransaction();

			for (String itemId : itemIds) {

				PersistentItem persistentItem = (PersistentItem) session.createCriteria(PersistentItem.class)
						.add(Restrictions.eq("itemId", itemId)).uniqueResult();
				if (persistentItem != null) {
					favoriteItems.add(convert(persistentItem));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return favoriteItems;
	}
	
	
	private PersistentItem convert(Item item) {
		PersistentItem persistentItem = new PersistentItem();
		persistentItem.setItemId(item.getItemId());
		persistentItem.setName(item.getName());
		persistentItem.setRating(item.getRating());
		persistentItem.setAddress(item.getAddress());
		persistentItem.setImageUrl(item.getImageUrl());
		persistentItem.setUrl(item.getUrl());
		persistentItem.setDistance(item.getDistance());
		persistentItem.setCategories(new HashSet<>());

		for (String category : item.getCategories()) {
			PersistentCategories persistentCategories = new PersistentCategories();
			persistentCategories.setCategoryId(new CategoryId(item.getItemId(), category));
			persistentCategories.setCategory(category);
			persistentCategories.setPersistentItem(persistentItem);
			persistentItem.getCategories().add(persistentCategories);
		}

		return persistentItem;
	}
	
	private Item convert(PersistentItem persistentItem) {
		ItemBuilder builder = new ItemBuilder();
		builder.setItemId(persistentItem.getItemId());
		builder.setName(persistentItem.getName());
		builder.setAddress(persistentItem.getAddress());
		builder.setDistance(persistentItem.getDistance());
		builder.setImageUrl(persistentItem.getImageUrl());
		builder.setUrl(persistentItem.getUrl());
		builder.setRating(persistentItem.getRating());
		Set<String> categories = new HashSet<>();

		for (PersistentCategories category : persistentItem.getCategories()) {
			categories.add(category.getCategory());
		}

		builder.setCategories(categories);
		return builder.build();
	}
}

