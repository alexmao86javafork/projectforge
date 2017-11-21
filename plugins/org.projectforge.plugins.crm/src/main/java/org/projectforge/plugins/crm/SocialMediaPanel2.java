/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.crm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.InstantMessagingType;
import org.projectforge.framework.utils.LabelValueBean;
import org.projectforge.web.wicket.components.AjaxMaxLengthEditableLabel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class SocialMediaPanel2 extends Panel
{

  /**
   *
   */
  private static final long serialVersionUID = -8335406879004963257L;

  @SpringBean
  private ContactDao contactDao;

  private List<SocialMediaValue> socialMediaValues = null;

  private RepeatingView socialMediaRepeater;

  private WebMarkupContainer mainContainer, addNewSocialMediaContainer;

  private LabelValueChoiceRenderer<InstantMessagingType> socialMediaChoiceRenderer;

  private LabelValueChoiceRenderer<SocialMediaType> socialMediaChoiceRenderer2;

  private LabelValueBean<InstantMessagingType, String> newSocialMediaValue;

  private final String DEFAULT_IM_VALUE = "Benutzer";

  private Component delete;

  private final IModel<ContactDO> model;

  /**
   * @param id
   */
  public SocialMediaPanel2(final String id, final IModel<ContactDO> model)
  {
    super(id);
    this.model = model;
    if (model != null && model.getObject() != null
        && StringUtils.isNotBlank(model.getObject().getSocialMediaValues()) == true) {
      socialMediaValues = contactDao.readSocialMediaValues(model.getObject().getSocialMediaValues());
    }
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    if (socialMediaValues == null) {
      socialMediaValues = new ArrayList<SocialMediaValue>();
    }
    newSocialMediaValue = new LabelValueBean<InstantMessagingType, String>(InstantMessagingType.AIM, DEFAULT_IM_VALUE);

    socialMediaChoiceRenderer = new LabelValueChoiceRenderer<InstantMessagingType>();
    final InstantMessagingType[] ims = InstantMessagingType.values();
    for (final InstantMessagingType im : ims) {
      socialMediaChoiceRenderer.addValue(im, im.toString());
    }

    mainContainer = new WebMarkupContainer("main");
    add(mainContainer.setOutputMarkupId(true));
    socialMediaRepeater = new RepeatingView("liRepeater");
    mainContainer.add(socialMediaRepeater);

    rebuildSocialMedias();
    addNewSocialMediaContainer = new WebMarkupContainer("liAddNewIm");
    mainContainer.add(addNewSocialMediaContainer.setOutputMarkupId(true));

    init(addNewSocialMediaContainer);
    socialMediaRepeater.setVisible(true);
  }

  @SuppressWarnings("serial")
  void init(final WebMarkupContainer item)
  {

    final DropDownChoice<InstantMessagingType> socialMediaChoice = new DropDownChoice<InstantMessagingType>(
        "socialMediaChoice", new PropertyModel<InstantMessagingType>(
        newSocialMediaValue, "label"),
        socialMediaChoiceRenderer.getValues(), socialMediaChoiceRenderer);
    item.add(socialMediaChoice);
    socialMediaChoice.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        //        newSocialMediaValue.setLabel(socialMediaChoice.getModelObject().get(socialMediaChoice.getModelObject().getKey()));
        //        model.getObject().setSocialMediaValues(contactDao.getSocialMediaValuesAsXml(socialMediaValues));
        //target.add(mainContainer);
      }
    });

    item.add(new AjaxMaxLengthEditableLabel("editableLabel", new PropertyModel<String>(newSocialMediaValue, "value"))
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        super.onSubmit(target);
        if (StringUtils.isNotBlank(newSocialMediaValue.getValue()) == true
            && newSocialMediaValue.getValue().equals(DEFAULT_IM_VALUE) == false) {
          model.getObject().setSocialMedia(newSocialMediaValue.getLabel(), newSocialMediaValue.getValue());
        }
        newSocialMediaValue.setValue(DEFAULT_IM_VALUE);
        rebuildSocialMedias();
        target.add(mainContainer);
      }
    });

    final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
    deleteDiv.setOutputMarkupId(true);
    deleteDiv.add(delete = new AjaxIconLinkPanel("delete", IconType.REMOVE,
        new PropertyModel<String>(newSocialMediaValue, "value"))
    {
      /**
       * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onClick(final AjaxRequestTarget target)
      {
        //        super.onClick(target);
        //        final Iterator<SocialMediaValue> it = socialMediaValues.iterator();
        //        while (it.hasNext() == true) {
        //          if (it.next() == newSocialMediaValue) {
        //            it.remove();
        //          }
        //        }
        //        rebuildSocialMedias();
        //        model.getObject().setSocialMediaValues(contactDao.getSocialMediaValuesAsXml(socialMediaValues));
        //        target.add(mainContainer);
      }
    });
    item.add(deleteDiv);
    delete.setVisible(false);
  }

  @SuppressWarnings("serial")
  private void rebuildSocialMedias()
  {
    socialMediaRepeater.removeAll();
    if (model.getObject().getSocialMedia() != null) {
      for (final LabelValueBean<InstantMessagingType, String> socialMediaValue : model.getObject().getSocialMedia()) {

        final WebMarkupContainer item = new WebMarkupContainer(socialMediaRepeater.newChildId());
        socialMediaRepeater.add(item);

        final DropDownChoice<InstantMessagingType> socialMediaChoice = new DropDownChoice<InstantMessagingType>(
            "socialMediaChoice", new PropertyModel<InstantMessagingType>(
            socialMediaValue, "label"),
            socialMediaChoiceRenderer.getValues(), socialMediaChoiceRenderer);
        item.add(socialMediaChoice);
        socialMediaChoice.add(new AjaxFormComponentUpdatingBehavior("change")
        {
          @Override
          protected void onUpdate(final AjaxRequestTarget target)
          {
            //          socialMediaValue.setSocialMediaType(socialMediaChoice.getModelObject());
            //          model.getObject().setSocialMediaValues(contactDao.getSocialMediaValuesAsXml(socialMediaValues));
            //target.add(mainContainer);
          }
        });

        item.add(new AjaxMaxLengthEditableLabel("editableLabel", new PropertyModel<String>(socialMediaValue, "value"))
        {
          @Override
          protected void onSubmit(final AjaxRequestTarget target)
          {
            super.onSubmit(target);
            model.getObject().setSocialMedia(socialMediaValue.getLabel(), socialMediaValue.getValue());
            rebuildSocialMedias();
            target.add(mainContainer);
          }
        });

        final WebMarkupContainer deleteDiv = new WebMarkupContainer("deleteDiv");
        deleteDiv.setOutputMarkupId(true);
        deleteDiv
            .add(new AjaxIconLinkPanel("delete", IconType.REMOVE, new PropertyModel<String>(socialMediaValue, "value"))
            {
              /**
               * @see org.projectforge.web.wicket.flowlayout.AjaxIconLinkPanel#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
               */
              @Override
              protected void onClick(final AjaxRequestTarget target)
              {
                //          super.onClick(target);
                //          final Iterator<SocialMediaValue> it = socialMediaValues.iterator();
                //          while (it.hasNext() == true) {
                //            if (it.next() == socialMediaValue) {
                //              it.remove();
                //            }
                //          }
                //          rebuildSocialMedias();
                //          model.getObject().setSocialMediaValues(contactDao.getSocialMediaValuesAsXml(socialMediaValues));
                //          target.add(mainContainer);
              }
            });
        item.add(deleteDiv);
      }
    }
  }

}
