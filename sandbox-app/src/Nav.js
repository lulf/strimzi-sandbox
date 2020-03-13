import React from 'react';
import Dashboard from './Dashboard.js';
import Register from './Register.js';
import { BrowserRouter, Route, NavLink, Switch } from 'react-router-dom';

function Nav() {
    return (
            <BrowserRouter>
            <Switch>
            <Route exact path="/">
            <div className="NavApp">
            <nav>
            <NavLink className="largeLinkGithub" to="/register">Register with GitHub ></NavLink>
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
