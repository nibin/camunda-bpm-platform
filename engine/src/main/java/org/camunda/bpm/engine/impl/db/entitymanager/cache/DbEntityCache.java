/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.db.entitymanager.cache;

import static org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState.DELETED_MERGED;
import static org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState.DELETED_PERSISTENT;
import static org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState.DELETED_TRANSIENT;
import static org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState.MERGED;
import static org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState.PERSISTENT;
import static org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState.TRANSIENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.db.DbEntity;


/**
 * A simple first level cache for {@link DbEntity Entities}.
 *
 * @author Daniel Meyer
 *
 */
public class DbEntityCache {

  /**
   * The cache itself: maps entity types (classes) to maps indexed by id (primary key).
   *
   * The motivation for indexing by type (class) is
   *
   * a) multiple entities of different types could have the same value as primary key. In the
   *    process engine, TaskEntity and HistoricTaskEntity have the same id value.
   *
   * b) performance (?)
   */
  protected Map<Class<?>, Map<String, CachedDbEntity>> cachedEntites = new HashMap<Class<?>, Map<String, CachedDbEntity>>();

  protected DbEntityCacheKeyMapping cacheKeyMapping;

  public DbEntityCache() {
    this.cacheKeyMapping = DbEntityCacheKeyMapping.emptyMapping();
  }

  public DbEntityCache(DbEntityCacheKeyMapping cacheKeyMapping) {
    this.cacheKeyMapping = cacheKeyMapping;
  }

  /**
   * get an object from the cache
   *
   * @param type the type of the object
   * @param id the id of the object
   * @return the object or 'null' if the object is not in the cache
   * @throws ProcessEngineException if an object for the given id can be found but is of the wrong type.
   */
  @SuppressWarnings("unchecked")
  public <T extends DbEntity> T get(Class<T> type, String id) {
    Class<?> cacheKey = cacheKeyMapping.getEntityCacheKey(type);
    CachedDbEntity cachedDbEntity = getCachedEntity(cacheKey, id);
    if(cachedDbEntity != null) {
      DbEntity dbEntity = cachedDbEntity.getEntity();
      try {
        return (T) dbEntity;
      } catch(ClassCastException e) {
        throw new ProcessEngineException("Could not lookup entity of type '"+type+"' and id '"+id+"': found entity of type '"+dbEntity.getClass()+"'.", e);
      }
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends DbEntity> List<T> getEntitiesByType(Class<T> type) {
    Class<?> cacheKey = cacheKeyMapping.getEntityCacheKey(type);
    Map<String, CachedDbEntity> entities = cachedEntites.get(cacheKey);
    List<T> result = new ArrayList<T>();
    if(entities == null) {
      return Collections.emptyList();
    } else {
      for (CachedDbEntity cachedEntity : entities.values()) {
        if (type != cacheKey) {
          // if the cacheKey of this type differs from the actual type,
          // not all cached entites with the key should be returned.
          // Then we only add those entities whose type matches the argument type.
          if (type.isAssignableFrom(cachedEntity.getClass())) {
            result.add((T) cachedEntity.getEntity());
          }
        } else {
          result.add((T) cachedEntity.getEntity());
        }

      }
      return result;
    }
  }

  /**
   * Looks up an entity in the cache.
   *
   * @param type the type of the object
   * @param id the id of the CachedEntity to lookup
   * @return the cached entity or null if the entity does not exist.
   */
  public CachedDbEntity getCachedEntity(Class<?> type, String id) {
    Class<?> cacheKey = cacheKeyMapping.getEntityCacheKey(type);
    Map<String, CachedDbEntity> entitesByType = cachedEntites.get(cacheKey);
    if(entitesByType != null) {
      return entitesByType.get(id);
    } else {
      return null;
    }
  }

  /**
   * Looks up an entity in the cache.
   * @param dbEntity the entity for which the CachedEntity should be looked up
   * @return the cached entity or null if the entity does not exist.
   */
  public CachedDbEntity getCachedEntity(DbEntity dbEntity) {
    return getCachedEntity(dbEntity.getClass(), dbEntity.getId());
  }

  /**
   * Put a new, {@link DbEntityState#TRANSIENT} object into the cache.
   *
   * @param e the object to put into the cache
   */
  public void putTransient(DbEntity e) {
    CachedDbEntity cachedDbEntity = new CachedDbEntity();
    cachedDbEntity.setEntity(e);
    cachedDbEntity.setEntityState(TRANSIENT);
    putInternal(cachedDbEntity);
  }

  /**
   * Put a {@link DbEntityState#PERSISTENT} object into the cache.
   *
   * @param e the object to put into the cache
   */
  public void putPersistent(DbEntity e) {
    CachedDbEntity cachedDbEntity = new CachedDbEntity();
    cachedDbEntity.setEntity(e);
    cachedDbEntity.setEntityState(PERSISTENT);
    cachedDbEntity.makeCopy();

    putInternal(cachedDbEntity);
  }

  /**
   * Put a {@link DbEntityState#MERGED} object into the cache.
   *
   * @param e the object to put into the cache
   */
  public void putMerged(DbEntity e) {
    CachedDbEntity cachedDbEntity = new CachedDbEntity();
    cachedDbEntity.setEntity(e);
    cachedDbEntity.setEntityState(MERGED);
    // no copy required

    putInternal(cachedDbEntity);
  }

  protected void putInternal(CachedDbEntity entityToAdd) {
    Class<? extends DbEntity> type = entityToAdd.getEntity().getClass();
    Class<?> cacheKey = cacheKeyMapping.getEntityCacheKey(type);

    Map<String, CachedDbEntity> map = cachedEntites.get(cacheKey);
    if(map == null) {
      map = new HashMap<String, CachedDbEntity>();
      cachedEntites.put(cacheKey, map);
    }

    // check whether this object is already present in the cache
    CachedDbEntity existingCachedEntity = map.get(entityToAdd.getEntity().getId());
    if(existingCachedEntity == null) {
      // no such entity exists -> put it into the cache
      map.put(entityToAdd.getEntity().getId(), entityToAdd);

    } else {
      // the same entity is already cached
      switch (entityToAdd.getEntityState()) {

      case TRANSIENT:
        // cannot put TRANSIENT entity if entity with same id already exists in cache.
        if(existingCachedEntity.getEntityState() == TRANSIENT) {
          throw new ProcessEngineException("Same entity with Id "+entityToAdd.getEntity().getId()
              + " and type "+entityToAdd.getEntity().getClass()+ " is inserted twice.");
        }
        else {
          throw new ProcessEngineException("Inserting entity with Id "+entityToAdd.getEntity().getId()
              + " and type "+entityToAdd.getEntity().getClass()+ " wich is already marked " + existingCachedEntity.getEntityState());
        }

      case PERSISTENT:
        if(existingCachedEntity.getEntityState() == PERSISTENT) {
          // use new entity state, replacing the existing one.
          map.put(entityToAdd.getEntity().getId(), entityToAdd);
          break;
        }
        if(existingCachedEntity.getEntityState() == DELETED_PERSISTENT
            || existingCachedEntity.getEntityState() == DELETED_MERGED) {
          // ignore put -> this is already marked to be deleted
          break;
        }

        // otherwise fail:
        throw new ProcessEngineException("Cannot add PERSISTENT entity with id "+entityToAdd.getEntity().getId()
            + " and type "+entityToAdd.getEntity().getClass() + " into cache: entity with same Id and type is already "
            + existingCachedEntity.getEntityState());

      case MERGED:
        if(existingCachedEntity.getEntityState() == PERSISTENT
            || existingCachedEntity.getEntityState() == MERGED) {
          // use new entity state, replacing the existing one.
          map.put(entityToAdd.getEntity().getId(), entityToAdd);
          break;
        }
        if(existingCachedEntity.getEntityState() == DELETED_PERSISTENT
            || existingCachedEntity.getEntityState() == DELETED_MERGED) {
          // ignore put -> this is already marked to be deleted
          break;
        }

        // otherwise fail:
        throw new ProcessEngineException("Cannot add MERGED entity with id "+entityToAdd.getEntity().getId()
            + " and type "+entityToAdd.getEntity().getClass() + " into cache: entity with same Id and type is already "
            + existingCachedEntity.getEntityState());

      default:
        // deletes are always added
        map.put(entityToAdd.getEntity().getId(), entityToAdd);
        break;
      }
    }
  }

  /**
   * Remove an entity from the cache
   * @param e the entity to remove
   * @return
   */
  public boolean remove(DbEntity e) {
    Class<?> cacheKey = cacheKeyMapping.getEntityCacheKey(e.getClass());
    Map<String, CachedDbEntity> typeMap = cachedEntites.get(cacheKey);
    if(typeMap != null) {
      return typeMap.remove(e.getId()) != null;
    } else {
      return false;
    }
  }

  /**
   * @param cachedDbEntity
   */
  public void remove(CachedDbEntity cachedDbEntity) {
    remove(cachedDbEntity.getEntity());
  }

  /**
   * Allows checking whether the provided entity is present in the cache
   *
   * @param dbEntity the entity to check
   * @return true if the the provided entity is present in the cache
   */
  public boolean contains(DbEntity dbEntity) {
    return getCachedEntity(dbEntity) != null;
  }

  /**
   * Allows checking whether the provided entity is present in the cache
   * and is {@link DbEntityState#PERSISTENT}.
   *
   * @param dbEntity the entity to check
   * @return true if the provided entity is present in the cache and is
   * {@link DbEntityState#PERSISTENT}.
   */
  public boolean isPersistent(DbEntity dbEntity) {
    CachedDbEntity cachedDbEntity = getCachedEntity(dbEntity);
    if(cachedDbEntity == null) {
      return false;
    } else {
      return cachedDbEntity.getEntityState() == PERSISTENT;
    }
  }

  /**
   * Allows checking whether the provided entity is present in the cache
   * and is marked to be deleted.
   *
   * @param dbEntity the entity to check
   * @return true if the provided entity is present in the cache and is
   * marked to be deleted
   */
  public boolean isDeleted(DbEntity dbEntity) {
    CachedDbEntity cachedDbEntity = getCachedEntity(dbEntity);
    if(cachedDbEntity == null) {
      return false;
    } else {
      return cachedDbEntity.getEntityState() == DELETED_MERGED
          || cachedDbEntity.getEntityState() == DELETED_PERSISTENT
          || cachedDbEntity.getEntityState() == DELETED_TRANSIENT;
    }
  }

  /**
   * Allows checking whether the provided entity is present in the cache
   * and is {@link DbEntityState#TRANSIENT}.
   *
   * @param dbEntity the entity to check
   * @return true if the provided entity is present in the cache and is
   * {@link DbEntityState#TRANSIENT}.
   */
  public boolean isTransient(DbEntity dbEntity) {
    CachedDbEntity cachedDbEntity = getCachedEntity(dbEntity);
    if(cachedDbEntity == null) {
      return false;
    } else {
      return cachedDbEntity.getEntityState() == TRANSIENT;
    }
  }

  public List<CachedDbEntity> getCachedEntities() {
    List<CachedDbEntity> result = new ArrayList<CachedDbEntity>();
    for (Map<String, CachedDbEntity> typeCache : cachedEntites.values()) {
      result.addAll(typeCache.values());
    }
    return result;
  }

  /**
   * Sets an object to a deleted state. It will not be removed from the cache but
   * transition to one of the DELETED states, depending on it's current state.
   *
   * @param dbEntity the object to mark deleted.
   */
  public void setDeleted(DbEntity dbEntity) {
    CachedDbEntity cachedEntity = getCachedEntity(dbEntity);
    if(cachedEntity != null) {
      if(cachedEntity.getEntityState() == TRANSIENT) {
        cachedEntity.setEntityState(DELETED_TRANSIENT);

      } else if(cachedEntity.getEntityState() == PERSISTENT){
        cachedEntity.setEntityState(DELETED_PERSISTENT);

      } else if(cachedEntity.getEntityState() == MERGED){
        cachedEntity.setEntityState(DELETED_MERGED);
      }
    } else {
      // put a deleted merged into the cache
      CachedDbEntity cachedDbEntity = new CachedDbEntity();
      cachedDbEntity.setEntity(dbEntity);
      cachedDbEntity.setEntityState(DELETED_MERGED);
      putInternal(cachedDbEntity);

    }
  }

}
