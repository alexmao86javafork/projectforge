/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.orga;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.MessageParam;
import org.projectforge.framework.i18n.MessageParamType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class ContractDao extends BaseDao<ContractDO> {
  public final static int START_NUMBER = 1000;

  public static final UserRightId USER_RIGHT_ID = UserRightId.ORGA_CONTRACTS;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContractDao.class);

  public ContractDao() {
    super(ContractDO.class);
    userRightId = USER_RIGHT_ID;
  }

  private static final String[] ENABLED_AUTOCOMPLETION_PROPERTIES = {"title", "coContractorA", "coContractorB", "contractPersonA", "contractPersonB", "signerA", "signerB"};

  @Override
  public boolean isAutocompletionPropertyEnabled(String property) {
    // All users with select access for contracts have access to all contracts, therefore no special select checking for single entities is needed.
    return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property);
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<ContractDO> getList(final BaseSearchFilter filter) throws AccessException {
    final ContractFilter myFilter;
    if (filter instanceof ContractFilter) {
      myFilter = (ContractFilter) filter;
    } else {
      myFilter = new ContractFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getStatus() != null) {
      queryFilter.add(Restrictions.eq("status", myFilter.getStatus().name()));
    }
    if (myFilter.getType() != null) {
      queryFilter.add(Restrictions.eq("type", myFilter.getType().getValue()));
    }
    queryFilter.setYearAndMonth("date", myFilter.getYear(), -1);
    if (log.isDebugEnabled() == true) {
      log.debug(myFilter.toString());
    }
    return getList(queryFilter);
  }

  /**
   * List of all years with contracts: select min(date), max(date) from t_contract.
   *
   * @return
   */
  public int[] getYears() {
    final java.sql.Date[] minMaxDate = getSession().createNamedQuery(ContractDO.SELECT_MIN_MAX_DATE, java.sql.Date[].class)
            .getSingleResult();
    return SQLHelper.getYears(minMaxDate[0], minMaxDate[1]);
  }

  /**
   * A given contract number must be consecutively numbered.
   */
  @Override
  protected void onSaveOrModify(final ContractDO obj) {
    if (obj.getNumber() == null) {
      throw new UserException("validation.required.valueNotPresent", new MessageParam("legalAffaires.contract.number",
              MessageParamType.I18N_KEY)).setCausedByField("number");
    }
    if (obj.getId() == null) {
      // New contract
      final Integer next = getNextNumber(obj);
      if (next.intValue() != obj.getNumber().intValue()) {
        throw new UserException("legalAffaires.contract.error.numberNotConsecutivelyNumbered").setCausedByField("number");
      }
    } else {
      final List<ContractDO> list = getSession().createNamedQuery(ContractDO.FIND_OTHER_BY_NUMBER, ContractDO.class)
              .setParameter("number", obj.getNumber())
              .setParameter("id", obj.getId())
              .list();
      if (list != null && list.size() > 0) {
        throw new UserException("legalAffaires.contract.error.numberAlreadyExists").setCausedByField("number");
      }
    }
  }

  /**
   * Gets the highest contract number.
   *
   * @param contract is needed to check wether the contract does already exist or not. If already exist it will be
   *                 assured that this contract has an unchanged number.
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public Integer getNextNumber(final ContractDO contract) {
    if (contract.getId() != null) {
      final ContractDO orig = internalGetById(contract.getId());
      if (orig.getNumber() != null) {
        contract.setNumber(orig.getNumber());
        return orig.getNumber();
      }
    }
    final List<Integer> list = getSession().createQuery("select max(t.number) from ContractDO t").list();
    Validate.notNull(list);
    if (list.size() == 0 || list.get(0) == null) {
      log.info("First entry of ContractDO");
      return START_NUMBER;
    }
    final Integer number = list.get(0);
    return number + 1;
  }

  @Override
  public ContractDO newInstance() {
    return new ContractDO();
  }
}
