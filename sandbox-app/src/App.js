/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { Component } from 'react';
import './App.css';
import Header from './Header.js';
import Dashboard from './Dashboard.js';
import Register from './Register.js';
import Unregister from './Unregister.js';
import Login from './Login.js';
import DeleteUser from './DeleteUser.js';
import { BrowserRouter, Route, Switch } from 'react-router-dom';

class App extends Component {
    render() {
        return (
            <div>
            <Header />
            <BrowserRouter>
            <Switch>
            <Route exact path="/">
            <Dashboard />
            </Route>
            <Route path="/login">
            <Login />
            </Route>
            <Route path="/register">
            <Register />
            </Route>
            <Route path="/unregister">
            <Unregister />
            </Route>
            <Route path="/deleteuser">
            <DeleteUser />
            </Route>
            </Switch>
            </BrowserRouter>
            </div>
        );
    }
}

export default App;
