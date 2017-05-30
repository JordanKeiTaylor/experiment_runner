// Copyright (c) 2016 All Right Reserved, Improbable Worlds Ltd.

const path = require('path');
const fs = require('fs');
const webpack = require('webpack');

const REGEX = {
  js: /\.js$/,
  ts: /\.tsx?$/,
};

module.exports = {
  REGEX: REGEX,
  entry: {
    // Your project's entry point.
    "index": "./src/index.js",
  },
  output: {
    path: path.resolve(__dirname, 'build'),
    filename: 'demo.js',
  },
  devtool: 'inline-source-map',
  module: {
    loaders: [
      {
        test: REGEX.js,
        include: /src|node_modules/,
        loader: 'babel?cacheDirectory'
      },
      {
        test: REGEX.ts,
        include: /src|node_modules/,
        loader: "babel?cacheDirectory!ts-loader"
      }
    ]
  },
  plugins: [],
  resolve: {
    extensions: ["", ".js"]
  }
};
