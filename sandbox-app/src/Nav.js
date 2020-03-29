/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from 'react';
import Dashboard from './Dashboard.js';
import Register from './Register.js';
import TermsOfService from './TermsOfService.js';
import { BrowserRouter, Route, NavLink, Switch } from 'react-router-dom';

function Nav() {
    return (
            <BrowserRouter>
            <Switch>
            <Route exact path="/">
            <TermsOfService />
            <div className="NavApp">
            <nav>
            <NavLink className="largeLinkBlack" to="/register">Register with GitHub ></NavLink>
            &nbsp;
            &nbsp;
            &nbsp;
            &nbsp;
            <NavLink className="largeLink" to="/dashboard">Dashboard ></NavLink>
            </nav>
            </div>
            </Route>
            <Route path="/register">
            <Register />
            </Route>
            <Route path="/dashboard">
            <Dashboard />
            </Route>
            </Switch>
            </BrowserRouter>
    );
}

export default Nav;
