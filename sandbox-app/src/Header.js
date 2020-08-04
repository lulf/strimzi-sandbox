/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { Component } from 'react';
import logo from './logo.png';
import './App.css';

class Header extends Component {
  render() {
      return (
        <div className="App">
            <img src={logo} className="App-logo" alt="logo" />
            <p>
                Strimzi Sandbox is a public service for developers who want to try a managed Strimzi service without having to setting up Kubernetes.
            </p>
            <p>
                You can use a <a href="https://www.github.com">GitHub</a> account to register and get a free environment for a limited time period. Once expired, all data for the environment and account data will be deleted.
            </p>
            <p>
              For more information about Strimzi, go to <a href="https://strimzi.io/">strimzi.io</a>.
            </p>
        </div>);
  }
};

export default Header;
