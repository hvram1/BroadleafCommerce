/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.common.sandbox.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.broadleafcommerce.common.sandbox.domain.SandBox;
import org.broadleafcommerce.common.sandbox.domain.SandBoxImpl;
import org.broadleafcommerce.common.sandbox.domain.SandBoxManagement;
import org.broadleafcommerce.common.sandbox.domain.SandBoxManagementImpl;
import org.broadleafcommerce.common.sandbox.domain.SandBoxType;
import org.broadleafcommerce.common.util.TransactionUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

@Repository("blSandBoxDao")
public class SandBoxDaoImpl implements SandBoxDao {

    @PersistenceContext(unitName = "blPU")
    protected EntityManager sandBoxEntityManager;
    
    @Resource(name = "blTransactionManager")
    protected JpaTransactionManager transactionManager;

    @Override
    public SandBox retrieve(Long id) {
        //Need to not create a query through SandBoxManagement here. Otherwise, a Hibernate exception can occur
        //(i.e. org.hibernate.HibernateException: Found two representations of same collection: org.broadleafcommerce.core.catalog.domain.ProductImpl.additionalSkus
        //when saving a change to product).
        return sandBoxEntityManager.find(SandBoxImpl.class, id);
    }
    
    @Override
    public List<SandBox> retrieveAllSandBoxes() {
        CriteriaBuilder builder = sandBoxEntityManager.getCriteriaBuilder();
        CriteriaQuery<SandBox> criteria = builder.createQuery(SandBox.class);
        Root<SandBoxManagementImpl> sandbox = criteria.from(SandBoxManagementImpl.class);
        criteria.select(sandbox.get("sandBox").as(SandBox.class));
        TypedQuery<SandBox> query = sandBoxEntityManager.createQuery(criteria);
        return query.getResultList();
    }

    @Override
    public List<SandBox> retrieveSandBoxesByType(SandBoxType sandboxType) {
        CriteriaBuilder builder = sandBoxEntityManager.getCriteriaBuilder();
        CriteriaQuery<SandBox> criteria = builder.createQuery(SandBox.class);
        Root<SandBoxManagementImpl> sandbox = criteria.from(SandBoxManagementImpl.class);
        criteria.select(sandbox.get("sandBox").as(SandBox.class));
        criteria.where(builder.equal(sandbox.get("sandBox").get("sandboxType"), sandboxType.getType()));
        TypedQuery<SandBox> query = sandBoxEntityManager.createQuery(criteria);
        return query.getResultList();
    }

    @Override
    public SandBox retrieveUserSandBoxForParent(Long authorId, Long parentSandBoxId) {
        CriteriaBuilder builder = sandBoxEntityManager.getCriteriaBuilder();
        CriteriaQuery<SandBox> criteria = builder.createQuery(SandBox.class);
        Root<SandBoxManagementImpl> sandbox = criteria.from(SandBoxManagementImpl.class);
        criteria.select(sandbox.get("sandBox").as(SandBox.class));
        List<Predicate> restrictions = new ArrayList<Predicate>();
        restrictions.add(builder.equal(sandbox.get("sandBox").get("sandboxType"), SandBoxType.USER.getType()));
        restrictions.add(builder.equal(sandbox.get("sandBox").get("author"), authorId));
        restrictions.add(builder.equal(sandbox.get("sandBox").get("parentSandBox").get("id"), parentSandBoxId));
        criteria.where(restrictions.toArray(new Predicate[restrictions.size()]));

        TypedQuery<SandBox> query = sandBoxEntityManager.createQuery(criteria);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public SandBox retrieveNamedSandBox(SandBoxType sandboxType, String sandboxName) {
        CriteriaBuilder builder = sandBoxEntityManager.getCriteriaBuilder();
        CriteriaQuery<SandBox> criteria = builder.createQuery(SandBox.class);
        Root<SandBoxManagementImpl> sandbox = criteria.from(SandBoxManagementImpl.class);
        criteria.select(sandbox.get("sandBox").as(SandBox.class));
        List<Predicate> restrictions = new ArrayList<Predicate>();
        restrictions.add(builder.equal(sandbox.get("sandBox").get("sandboxType"), SandBoxType.USER.getType()));
        restrictions.add(builder.equal(sandbox.get("sandBox").get("name"), sandboxName));
        criteria.where(restrictions.toArray(new Predicate[restrictions.size()]));

        TypedQuery<SandBox> query = sandBoxEntityManager.createQuery(criteria);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Map<Long, String> retrieveAuthorNamesForSandBoxes(Set<Long> sandBoxIds) {
        Query query = sandBoxEntityManager.createQuery(
                "SELECT sb.sandBox.id, au.name " +
                "FROM org.broadleafcommerce.common.sandbox.domain.SandBoxManagementImpl sb, " +
                    "org.broadleafcommerce.openadmin.server.security.domain.AdminUserImpl au " +
                "WHERE sb.sandBox.author = au.id " +
                "AND sb.sandBox.id IN :sandBoxIds");
        query.setParameter("sandBoxIds", sandBoxIds);
        List<Object[]> results = query.getResultList();

        Map<Long, String> map = new HashMap<Long, String>();
        for (Object[] result : results) {
            map.put((Long) result[0], (String) result[1]);
        }

        return map;
    }

    @Override
    public List<SandBox> retrieveSandBoxesForAuthor(Long authorId) {
        CriteriaBuilder builder = sandBoxEntityManager.getCriteriaBuilder();
        CriteriaQuery<SandBox> criteria = builder.createQuery(SandBox.class);
        Root<SandBoxManagementImpl> sandbox = criteria.from(SandBoxManagementImpl.class);
        criteria.select(sandbox.get("sandBox").as(SandBox.class));
        criteria.where(builder.equal(sandbox.get("sandBox").get("author"), authorId));
        TypedQuery<SandBox> query = sandBoxEntityManager.createQuery(criteria);
        return query.getResultList();
    }

    @Override
    public SandBox persist(SandBox entity) {
        sandBoxEntityManager.persist(entity);
        sandBoxEntityManager.flush();
        return entity;
    }

    @Override
    public SandBox createSandBox(String sandBoxName, SandBoxType sandBoxType) {
        TransactionStatus status = TransactionUtils.createTransaction("createSandBox",
                        TransactionDefinition.PROPAGATION_REQUIRES_NEW, transactionManager);
        try {
            SandBox approvalSandbox = retrieveNamedSandBox(sandBoxType, sandBoxName);
            if (approvalSandbox == null) {
                approvalSandbox = new SandBoxImpl();
                approvalSandbox.setName(sandBoxName);
                approvalSandbox.setSandBoxType(sandBoxType);
                approvalSandbox = persist(approvalSandbox);

                SandBoxManagement mgmt = new SandBoxManagementImpl();
                mgmt.setSandBox(approvalSandbox);
                sandBoxEntityManager.persist(mgmt);
                sandBoxEntityManager.flush();
            }
            TransactionUtils.finalizeTransaction(status, transactionManager, false);
            return approvalSandbox;
        } catch (Exception ex) {
            TransactionUtils.finalizeTransaction(status, transactionManager, true);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public SandBox createUserSandBox(Long authorId, SandBox approvalSandBox) {
        TransactionStatus status = TransactionUtils.createTransaction("createSandBox",
                        TransactionDefinition.PROPAGATION_REQUIRES_NEW, transactionManager);
        try {
            SandBox userSandBox = new SandBoxImpl();
            userSandBox.setName(approvalSandBox.getName());
            userSandBox.setAuthor(authorId);
            userSandBox.setParentSandBox(approvalSandBox);
            userSandBox.setSandBoxType(SandBoxType.USER);
            userSandBox = persist(userSandBox);

            SandBoxManagement mgmt = new SandBoxManagementImpl();
            mgmt.setSandBox(userSandBox);
            sandBoxEntityManager.persist(mgmt);
            sandBoxEntityManager.flush();

            TransactionUtils.finalizeTransaction(status, transactionManager, false);
            return userSandBox;
        } catch (Exception ex) {
            TransactionUtils.finalizeTransaction(status, transactionManager, true);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public SandBox createDefaultSandBox() {
        TransactionStatus status = TransactionUtils.createTransaction("createSandBox",
                        TransactionDefinition.PROPAGATION_REQUIRES_NEW, transactionManager);
        try {
            SandBox defaultSB = new SandBoxImpl();
            defaultSB.setName("Default");
            defaultSB.setSandBoxType(SandBoxType.DEFAULT);
            defaultSB.setColor("#0B9098");
            defaultSB = persist(defaultSB);

            SandBoxManagement mgmt = new SandBoxManagementImpl();
            mgmt.setSandBox(defaultSB);
            sandBoxEntityManager.persist(mgmt);
            sandBoxEntityManager.flush();

            TransactionUtils.finalizeTransaction(status, transactionManager, false);
            return defaultSB;
        } catch (Exception ex) {
            TransactionUtils.finalizeTransaction(status, transactionManager, true);
            throw new RuntimeException(ex);
        }
    }

}
