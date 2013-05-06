/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.core.web.api.endpoint.order;

import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.web.api.wrapper.OrderWrapper;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.web.core.CustomerState;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * This endpoint depends on JAX-RS.  It should be extended by components that actually wish 
 * to provide an endpoint.  The annotations such as @Path, @Scope, @Context, @PathParam, @QueryParam, 
 * @GET, @POST, @PUT, and @DELETE are purposely not provided here to allow implementors finer control over 
 * the details of the endpoint.
 * <p/>
 * User: Kelly Tisdell
 * Date: 4/10/12
 */
public class OrderHistoryEndpoint implements ApplicationContextAware {

    @Resource(name="blOrderService")
    protected OrderService orderService;

    protected ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public List<OrderWrapper> findOrdersForCustomer(HttpServletRequest request,
            String orderStatus) {
        Customer customer = CustomerState.getCustomer(request);
        OrderStatus status = OrderStatus.getInstance(orderStatus);

        if (customer != null && status != null) {
            List<Order> orders = orderService.findOrdersForCustomer(customer, status);

            if (orders != null && !orders.isEmpty()) {
                List<OrderWrapper> wrappers = new ArrayList<OrderWrapper>();
                for (Order order : orders) {
                    OrderWrapper wrapper = (OrderWrapper) context.getBean(OrderWrapper.class.getName());
                    wrapper.wrap(order, request);
                    wrappers.add(wrapper);
                }

                return wrappers;
            }

            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
}
